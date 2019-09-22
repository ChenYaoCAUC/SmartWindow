package com.ysmilec.smart_window.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.ysmilec.smart_window.R;
import com.ysmilec.smart_window.model.CmdsBean;
import com.ysmilec.smart_window.model.DatasBean;
import com.ysmilec.smart_window.msg.Msg;
import com.ysmilec.smart_window.utils.CloudManager;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements Handler.Callback {

    private Switch switchWindowState;
    private TextView textViewWindowState;
    private TextView textViewIsRain;
    private Button buttonGetWindowStatus;
    private Button buttonGetIsRain;
    private CloudManager cloudManager = CloudManager.getInstance();
    private Handler handler = new Handler(this);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUi();
    }

    protected void initUi() {
        switchWindowState = findViewById(R.id.switchWindowState);
        textViewWindowState = findViewById(R.id.textViewWindowState);
        textViewIsRain = findViewById(R.id.textViewIsRain);
        buttonGetWindowStatus = findViewById(R.id.buttonGetWindowStatus);
        buttonGetIsRain = findViewById(R.id.buttonGetRain);
        switchWindowState.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                try {
                    setWindowStatus(isChecked);
                    if(isChecked)
                        switchWindowState.setTextOn("打开窗户");
                    else
                        switchWindowState.setTextOff("关闭窗户");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                }


            }

        });

        buttonGetWindowStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    getWindowStatus();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                }

            }
        });

        buttonGetIsRain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    getWindowIsRain();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    protected void setWindowStatus(boolean isWindowOpen) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
//        Callback都是新线程，想操作UI必须sendMessage才行
        cloudManager.setWindowStatus(isWindowOpen, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
//                Log.e("设置窗户状态失败")
                Log.e("failure","设置窗户状态失败");
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.code()==200){
                    String result = response.body().string();
                    Log.i("cmdresponse:",result);
//                解析云端返回的数据
                    Gson gson = new Gson();
                    try {
                        CmdsBean cmdsBean = gson.fromJson(result, CmdsBean.class);
                        if(cmdsBean.getErrno()==0){
                            String data = new String(Base64.decode(cmdsBean.getData().getCmd_resp(),Base64.NO_WRAP));
                            Log.i("cmd:",data);
                            if(data.equals("{\"open\"}")){
                                data = "窗户已打开";
                            }else if(data.equals("{\"close\"}")){
                                data = "窗户已关闭";
                            }
                            Message msg = handler.obtainMessage(Msg.CMD_RESPONSE_SUCCESS, data);
                            handler.sendMessage(msg);
                        }else{
                            String data = "设备不在线！";
                            Message msg = handler.obtainMessage(Msg.CMD_RESPONSE_FAILURE,data);
                            handler.sendMessage(msg);
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                        Message msg = handler.obtainMessage(Msg.CMD_RESPONSE_FAILURE, e);
                        handler.sendMessage(msg);
                    }

                }else{
                    //    Log.e("设置窗户状态失败")
                    Log.e("failure","设置窗户状态失败");
                }

            }
        });
    }
    protected void getWindowStatus() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        cloudManager.getWindowStatus(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e("failure","获取窗户状态失败");
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.code()==200){
                    String result = response.body().string();
                    Log.i("getStatusresponse:",result);
//                解析云端返回的数据
                    Gson gson = new Gson();
                    try {
                        DatasBean datasBean = gson.fromJson(result,DatasBean.class);
                        if(datasBean.getErrno()==0){
                            String data = datasBean.getData().getCurrent_value();
                            Log.i("窗户状态：",data);
                            Message msg = handler.obtainMessage(Msg.GET_WINDOWSTATUS_SUCCESS, data);
                            handler.sendMessage(msg);
                        }else{
                            String data = "无法获取窗户状态！";
                            Message msg = handler.obtainMessage(Msg.GET_WINDOWSTATUS_FAILURE,data);
                            handler.sendMessage(msg);
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                        Message msg = handler.obtainMessage(Msg.GET_WINDOWSTATUS_FAILURE, e);
                        handler.sendMessage(msg);
                    }
                }else{
                    //    Log.e("设置窗户状态失败")
                    Log.e("failure","获取窗户状态失败");
                }
            }
        });
    }

    protected void getWindowIsRain() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        cloudManager.getWindowIsrain(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e("failure","获取雨滴状态失败");
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.code()==200){
                    String result = response.body().string();
                    Log.i("getIsRainresponse:",result);
//                解析云端返回的数据
                    Gson gson = new Gson();
                    try {
                        DatasBean datasBean = gson.fromJson(result,DatasBean.class);
                        if(datasBean.getErrno()==0){
                            String data = datasBean.getData().getCurrent_value();
                            Message msg = handler.obtainMessage(Msg.GET_WINDOWISRAIN_SUCCESS, data);
                            Log.i("雨滴状态：",data);
                            handler.sendMessage(msg);
                        }else{
                            String data = "无法获取雨滴状态！";
                            Message msg = handler.obtainMessage(Msg.GET_WINDOWISRAIN_FAILURE,data);
                            handler.sendMessage(msg);
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                        Message msg = handler.obtainMessage(Msg.GET_WINDOWISRAIN_FAILURE, e);
                        handler.sendMessage(msg);
                    }
                }else{
                    //    Log.e("设置窗户状态失败")
                    Log.e("failure","获取窗户状态失败");
                }
            }
        });
    }


//    主线程，Android必须在主线程更新UI，不然会抛出异常
    @Override
    public boolean handleMessage(@NonNull Message message) {
        switch (message.what) {
//            处理消息
            case Msg.CMD_RESPONSE_SUCCESS:
                Toast.makeText(MainActivity.this, (String) message.obj,Toast.LENGTH_LONG).show();
                break;
            case Msg.CMD_RESPONSE_FAILURE:
                Toast.makeText(MainActivity.this, (String) message.obj,Toast.LENGTH_LONG).show();
                break;
            case Msg.GET_WINDOWISRAIN_SUCCESS:
                if(message.obj instanceof String) {
                    String data = String.valueOf(message.obj);
                    if(data.equals("true")){
                        data = "有雨";
                    }else{
                        data = "晴朗";
                    }
                    Log.i("handelMessageIsRain:",data);
                    textViewIsRain.setText(data);
                }
                Toast.makeText(MainActivity.this, (String) message.obj,Toast.LENGTH_LONG).show();
                break;
            case Msg.GET_WINDOWISRAIN_FAILURE:
                Toast.makeText(MainActivity.this, (String) message.obj,Toast.LENGTH_LONG).show();
                break;
            case Msg.GET_WINDOWSTATUS_SUCCESS:
                if(message.obj instanceof String) {
                    String data = String.valueOf(message.obj);
                    if(data.equals("true")){
                        data = "打开";
                    }else{
                        data = "关闭";
                    }
                    Log.i("handelMessageStatus:",data);
                    textViewWindowState.setText(data);
                }
                Toast.makeText(MainActivity.this, (String) message.obj,Toast.LENGTH_LONG).show();
                break;
            case Msg.GET_WINDOWSTATUS_FAILURE:
                Toast.makeText(MainActivity.this, (String) message.obj,Toast.LENGTH_LONG).show();
                break;
            default:
                break;
        }
        return false;
    }
}
