package com.ysmilec.smart_window.model;


public class CmdsBean {
    private int errno;
    private String error;
    private DatasCmdBean data;

    public int getErrno() {
        return errno;
    }

    public void setErrno(int errno) {
        this.errno = errno;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public DatasCmdBean getData() {
        return data;
    }

    public void setData(DatasCmdBean data) {
        this.data = data;
    }
}
