import base64
import hmac
import time
import json
import threading
from urllib.parse import quote
import paho.mqtt.client as mqtt
import RPi.GPIO as GPIO

HOST = '183.230.40.16'  # 服务器地址
PORT = 8883  # 服务器端口
productid = '275459'  # 产品ID
devId = ''  # 设备ID
clientid = 'smartwindow_pi'  # Client ID
username = productid  # 用户名和产品ID一样
access_key = '5DcrdeJmlpeY7O1C8He7qgcGS3185XcCe6gCFeX4vyE='  # 这里的是设备级AccessKey，后面可能还需要产品AccessKey
global_id = 1

window_pin_num = 12
rain_pin_num = 11
light_pin_num = 15
voice_pin_num = 7

is_user_control = False
is_window_open = True
is_rain = False
is_noisy = False
is_glare = False
is_sensor = True


class WindowManager:
    @staticmethod
    def init():
        GPIO.setmode(GPIO.BOARD)
        GPIO.setup(rain_pin_num, GPIO.IN)
        GPIO.setup(light_pin_num, GPIO.IN)
        GPIO.setup(voice_pin_num, GPIO.IN)

    @staticmethod
    def control_window(open):
        global is_window_open
        is_window_open = open
        if open:
            GPIO.setup(window_pin_num, GPIO.IN)
        else:
            GPIO.setup(window_pin_num, GPIO.OUT)

    @staticmethod
    def rain():
        # 不下雨
        if GPIO.input(rain_pin_num):
            return False
        # 下雨
        else:
            return True

    @staticmethod
    def noisy():
        # 不吵闹
        if GPIO.input(voice_pin_num):
            return False
        # 吵闹
        else:
            return True


    @staticmethod
    def glare():
        # 不刺眼
        if GPIO.input(light_pin_num):
            return False
        # 刺眼
        else:
            return True


    @staticmethod
    def destroy():
        GPIO.output(window_pin_num, GPIO.LOW)
        GPIO.cleanup()


class MqttsClient:
    @staticmethod
    def token_generator(name, access_key):

        version = '2018-10-31'

        # res = 'products/%s' % name  # API访问（需要产品AccessKey）
        # res = 'mqs/%s' % name  # 消息队列MQ连接（暂时没用）
        res = 'products/%s/devices/%s' % (name, clientid)  # MQTTS设备连接（设备级AccessKey）

        # 用户自定义token过期时间
        et = str(int(time.time()) + 3600)

        # 签名方法，支持md5、sha1、sha256
        method = 'sha1'

        # 对access_key进行decode
        key = base64.b64decode(access_key)

        # 计算sign
        org = et + '\n' + method + '\n' + res + '\n' + version
        sign_b = hmac.new(key=key, msg=org.encode(), digestmod=method)
        sign = base64.b64encode(sign_b.digest()).decode()

        # value 部分进行url编码，method/res/version值较为简单无需编码
        sign = quote(sign, safe='')
        res = quote(res, safe='')
        # token参数拼接
        token = 'version=%s&res=%s&et=%s&method=%s&sign=%s' % (version, res, et, method, sign)
        return token

    @staticmethod
    def on_message(client, userdata, msg):
        if msg.topic == '$sys/275459/smartwindow_pi/dp/post/json/accepted':
            res = json.loads(msg.payload.decode('ascii'))
            print('Post accepted: '+str(res['id']))
        elif msg.topic == '$sys/275459/smartwindow_pi/dp/post/json/rejected':
            res = json.loads(msg.payload.decode('ascii'))
            print('Post rejected: ' + str(res['id']))
        elif msg.topic.startswith('$sys/275459/smartwindow_pi/cmd/request/'):
            response_topic = msg.topic.replace('request', 'response')
            # 自己定义发送格式，自己定义解析方式
            res = msg.payload.decode('ascii')
            print('Receive command: ' + res)
            global is_user_control
            if res == 'open' or res == '{"open"}':
                # 若用户的操作和传感器要做的事相反，则进入用户控制模式，暂时忽略传感器
                # 若相同，则解锁传感器
                if not is_window_open:
                    is_user_control = True
                else:
                    is_user_control = False
                WindowManager.control_window(True)
            elif res == 'close' or res == '{"close"}':
                if is_window_open:
                    is_user_control = True
                else:
                    is_user_control = False
                WindowManager.control_window(False)

            client.publish(response_topic, 'OK', 1)
        elif msg.topic.startswith('$sys/275459/smartwindow_pi/cmd/response/'):
            if msg.topic.endswith('accepted'):
                print('Command response accepted!')
            elif msg.topic.endswith('rejected'):
                print('Command response rejected!')
        else:
            print("Unknown Message: %s %s" % (msg.topic, msg.payload))

    @staticmethod
    def on_connect(client, userdata, flags, rc):
        print('Connected with result code '+str(rc))

    @staticmethod
    def on_disconnect(client, userdata, rc):
        print('Disconnected with result code ' + str(rc))

    @staticmethod
    def on_subscribe(client, userdata, mid, granted_qos):
        print('Subscribe success!')

    @staticmethod
    def on_publish(client, userdata, mid):
        print('Publish success!')

    @staticmethod
    def publish_status(is_open):
        global global_id
        payload_status = {
            "id": global_id,
            "dp": {
                "isopen": [{"v": is_open}]
            }
        }
        global_id = global_id + 1
        topic = "$sys/275459/smartwindow_pi/dp/post/json"
        client.publish(topic, json.dumps(payload_status), 1)

    @staticmethod
    def publish_glare(is_glare):
        global global_id
        payload_glare = {
            "id": global_id,
            "dp": {
                "isglare": [{"v": is_glare}]
            }
        }
        global_id = global_id + 1
        topic = "$sys/275459/smartwindow_pi/dp/post/json"
        client.publish(topic, json.dumps(payload_glare), 1)

    
    @staticmethod
    def publish_noisy(is_noisy):
        global global_id
        payload_noisy = {
            "id": global_id,
            "dp": {
                "isnoisy": [{"v": is_noisy}]
            }
        }
        global_id = global_id + 1
        topic = "$sys/275459/smartwindow_pi/dp/post/json"
        client.publish(topic, json.dumps(payload_noisy), 1)

    @staticmethod
    def publish_israin(is_rain):
        global global_id
        payload_rain = {
            "id": global_id,
            "dp": {
                "israin": [{"v": is_rain}]
            }
        }
        global_id = global_id + 1
        topic = "$sys/275459/smartwindow_pi/dp/post/json"
        client.publish(topic, json.dumps(payload_rain), 1)

    @staticmethod
    def publish_daemon():
        global is_window_open
        global is_rain
        global is_user_control
        global is_noisy
        global is_glare
        global is_sensor
        while True:
            is_rain = WindowManager.rain()
            is_noisy = WindowManager.noisy()
            is_glare = WindowManager.glare()
            print("是否吵闹：")
            print(is_noisy)
            print("是否刺眼：")
            print(is_glare)
            print("是否下雨：")
            print(is_rain)
            if is_rain is False and is_glare is False and is_noisy is False :
                is_sensor = True
            else:
                is_sensor = False
            # 若传感器判断和用户的操作相同，则自解锁
            if is_sensor is is_window_open:
                is_user_control = False
            if not is_user_control:
                WindowManager.control_window(is_sensor)
            MqttsClient.publish_israin(is_rain)
            MqttsClient.publish_noisy(is_noisy)
            MqttsClient.publish_glare(is_glare)
            MqttsClient.publish_status(is_window_open)
            time.sleep(5)

# 开关窗逻辑
# 保证所有的窗户开关和传感器必须在一个线程里执行，必须是串行
# 给窗户开关做一个队列
# 下雨（true）关窗户
# 阳光直射（true）关窗户
# 吵闹（true）关窗户
# 只要下雨就关窗户
# 不下雨但是阳光直射或者噪声就关窗户


if __name__ == '__main__':
    WindowManager.init()
    client = mqtt.Client()
    client.reinitialise(client_id=clientid, clean_session=True, userdata=None)
    pwd = MqttsClient.token_generator(productid, access_key)
    print(pwd)
    client.username_pw_set(username, pwd)
    client.tls_set(ca_certs='MQTTS-certificate.pem', certfile=None, keyfile=None, cert_reqs=None, tls_version=None, ciphers=None)
    # 暂时忽略主机名验证，不然会报 hostname '183.230.40.16' doesn't match 'OneNET MQTTS'
    client.tls_insecure_set(True)
    client.on_message = MqttsClient.on_message
    client.on_subscribe = MqttsClient.on_subscribe
    client.on_connect = MqttsClient.on_connect
    client.on_disconnect = MqttsClient.on_disconnect
    client.on_publish = MqttsClient.on_publish
    client.connect(HOST, PORT, 60)
    client.subscribe("$sys/275459/smartwindow_pi/dp/post/json/accepted")
    client.subscribe("$sys/275459/smartwindow_pi/dp/post/json/rejected")
    client.subscribe("$sys/275459/smartwindow_pi/cmd/request/+")
    client.subscribe("$sys/275459/smartwindow_pi/cmd/response/+/accepted")
    client.subscribe("$sys/275459/smartwindow_pi/cmd/response/+/rejected")
    # 本地控制，根据是否下雨来控制窗户开关
    t = threading.Thread(target=MqttsClient.publish_daemon)
    t.start()
    client.loop_forever()
