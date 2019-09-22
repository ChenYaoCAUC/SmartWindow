package com.ysmilec.smart_window.model;

public class DatasBean {
    private int errno;
    private String error;
    private DatastreamBean data;

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

    public DatastreamBean getData() {
        return data;
    }

    public void setData(DatastreamBean data) {
        this.data = data;
    }
}
