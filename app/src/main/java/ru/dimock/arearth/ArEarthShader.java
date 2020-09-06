package ru.dimock.arearth;

import android.content.res.Resources;
import android.opengl.GLES20;

import java.io.IOException;
import java.io.InputStream;

public class ArEarthShader {
    protected int program_;
    private ArEarthActivity context;

    ArEarthShader(ArEarthActivity c, int vshaderId, int fshaderId) {
        context = c;
        createShaders(vshaderId, fshaderId);
    }

    public int program() {
        return program_;
    }

    protected void createShaders(int vshaderId, int fshaderId) {
        String vertexShaderCode = readShaderCode(vshaderId);
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        if(vertexShader == 0)
            return;
        String fragmentShaderCode = readShaderCode(fshaderId);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        if(fragmentShader == 0) {
            throw new RuntimeException("Error creating shader.");
        }
        program_ = GLES20.glCreateProgram();
        GLES20.glAttachShader(program_, vertexShader);
        GLES20.glAttachShader(program_, fragmentShader);
    }

    protected String readShaderCode(int shaderId) {
        try {
            Resources res = context.getResources();
            InputStream is = res.openRawResource(shaderId);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    private int compileShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        final int[] status = new int [1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if(status[0] == 0) {
            String errorMsg = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }
}
