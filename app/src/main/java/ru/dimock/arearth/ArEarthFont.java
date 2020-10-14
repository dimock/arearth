package ru.dimock.arearth;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.util.Pair;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ArEarthFont {
    private ArEarthActivity context_ = null;
    private static final int COORDS_PER_VERTEX = 3;
    private static final int INDICES_PER_TRI = 3;
    private static final int TEXCOORDS_PER_VERTEX = 2;
    private FloatBuffer vertices;
    private float [] charTextureCoordinates = new float[4 * (128 - 32)];
    private float charBottoms[] = new float[128 - 32];
    private float charHeights[] = new float[128 - 32];
    private float charXYratio[] = new float[128 - 32];
    private ShortBuffer triangles;
    private float color[] = { 1.0f, 1.0f, 1.0f, 1.0f };
    private int program_;
    private int vertexStride;
    private int texcoordStride;
    private FontStyle style_;
    private int fontSize_ = 0;
    private ArEarthTexture texture_ = null;
    private FloatBuffer textureCoordinates = null;
    private Matrix4 matrixMVP = new Matrix4();
    private Vector3 pos_ = new Vector3();
    private RectF textSize_ = new RectF();
    private float viewportRatio_ = 0;

    public ArEarthFont(ArEarthActivity context, int fontSize, FontStyle style) {
        context_ = context;
        fontSize_ = fontSize;
        style_ = style;
        vertexStride = COORDS_PER_VERTEX * Float.BYTES;
        texcoordStride = TEXCOORDS_PER_VERTEX * Float.BYTES;
        ArEarthShader shader = new ArEarthShader(context, R.raw.vshaderfn, R.raw.fshaderfn);
        program_ = shader.program();
        GLES20.glBindAttribLocation(program_, 0, "vPosition");
        GLES20.glBindAttribLocation(program_, 1, "vTexCoords");
        GLES20.glLinkProgram(program_);
        createFontTexture();
    }

    public void setColor(int r, int g, int b, int a) {
        color[0] = (float)r / 255.0f;
        color[1] = (float)g / 255.0f;
        color[2] = (float)b / 255.0f;
        color[3] = (float)a / 255.0f;
    }

    public void updateViewportRatio(float ratio) {
        viewportRatio_ = ratio;
    }

    private void createFontTexture() {
        Paint textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setARGB(255, 255, 255, 255);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setStrokeWidth(0);
        if(style_ == FontStyle.Bold) {
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }
        else {
            textPaint.setTypeface(Typeface.DEFAULT);
        }

        float density = context_.getResources().getDisplayMetrics().density;
        textPaint.setTextSize(fontSize_ * density);
        textPaint.setTextAlign(Paint.Align.LEFT);
        Rect bounds = new Rect();

        char [] c = new char[2];
        c[1] = '\0';
        int width = 0;
        int height = 0;
        float avgwidth = 0;
        List<Pair<Integer, Integer>> characters = new ArrayList<>();
        for(c[0] = 32; c[0] < 128; ++c[0]) {
            textPaint.getTextBounds(c, 0, 1, bounds);
            width += bounds.width();
            height = Math.max(height, bounds.height());
            avgwidth += bounds.width();
            characters.add(new Pair<Integer, Integer>((int)c[0], Math.max(bounds.height(), 1)));
        }
        Collections.sort(characters, new Comparator<Pair<Integer, Integer>>() {
            @Override
            public int compare(Pair<Integer, Integer> p1, Pair<Integer, Integer> p2) {
                return Integer.compare(p2.second, p1.second);
            }
        });
        avgwidth /= 128 - 32;
        avgwidth = (int)avgwidth;
        width += avgwidth;
        double square = width * height;
        int twidth = 1 << (int)(Math.log(Math.sqrt(square)) / Math.log(2.0) + 1);
        int theight = 1 << (int)(Math.log(square / twidth) / Math.log(2.0) + 1);
        float twidth1 = 1.0f / twidth;
        float theight1 = 1.0f / theight;
        Bitmap bitmap = Bitmap.createBitmap(twidth, theight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        bitmap.eraseColor(0);

        int x = 0;
        int y = 0;
        for(Pair<Integer, Integer> ch : characters) {
            int i = ch.first.intValue();
            c[0] = (char)i;
            i -= 32;
            textPaint.getTextBounds(c, 0, 1, bounds);
            float curwidth  = bounds.width();
            float curheight = bounds.height();
            if(curwidth == 0) {
                curwidth = avgwidth;
                curheight = ch.second.intValue();
            }
            if(x + curwidth >= twidth) {
                x = 0;
            }
            if(x == 0) {
                y += ch.second.intValue();
                if(y >= theight)
                    break;
            }
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setStrokeWidth(0);
            canvas.drawText(c, 0, 1, x - bounds.left, y - bounds.bottom, textPaint);
            float x0 = x;
            float y0 = y - curheight;
            float x1 = x0 + curwidth;
            float y1 = y;
            charBottoms[i] = (float)bounds.bottom / height;
            charHeights[i] = curheight / height;
            charXYratio[i] = curwidth / curheight;
            charTextureCoordinates[i*4 + 0] = x0 * twidth1;
            charTextureCoordinates[i*4 + 1] = y0 * theight1;
            charTextureCoordinates[i*4 + 2] = x1 * twidth1;
            charTextureCoordinates[i*4 + 3] = y1 * theight1;
            x += curwidth;
        }
        texture_ = new ArEarthTexture(bitmap);
    }

    private void updateTextBuffers(String text) {
        int pointsN = text.length() * 4;
        int trisN = text.length() * 2;
        int verticesN = pointsN * COORDS_PER_VERTEX ;
        int indicesN = trisN * INDICES_PER_TRI;
        if(vertices == null || vertices.capacity() < verticesN) {
            ByteBuffer bb = ByteBuffer.allocateDirect(verticesN * Float.BYTES);
            bb.order(ByteOrder.nativeOrder());
            vertices = bb.asFloatBuffer();
            vertices.position(0);

            ByteBuffer txb = ByteBuffer.allocateDirect(verticesN * Float.BYTES);
            txb.order(ByteOrder.nativeOrder());
            textureCoordinates = txb.asFloatBuffer();
            textureCoordinates.position(0);

            ByteBuffer tbb = ByteBuffer.allocateDirect(indicesN * Short.BYTES);
            tbb.order(ByteOrder.nativeOrder());
            triangles = tbb.asShortBuffer();
            triangles.position(0);
        }
    }

    private boolean createTextBuffers(String text) {
        if(text.length() == 0) {
            return false;
        }
        updateTextBuffers(text);
        return true;
    }

    private void fillTextBuffers(String text, float h) {
        if(text.length() == 0) {
            return;
        }

        int lines = 1;
        for(int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if(c == '\n') {
                lines++;
            }
        }

        float x = 0;
        float y = lines * h;

        boolean bboxInitialized = false;
        for(int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if(c == '\n') {
                x = 0;
                y -= h;
                continue;
            }
            if(c < 32) {
                c = 32;
            }
            if(c > 127) {
                c = 127;
            }
            int j = c - 32;
            float tx0 = charTextureCoordinates[j*4 + 0];
            float ty0 = charTextureCoordinates[j*4 + 1];
            float tx1 = charTextureCoordinates[j*4 + 2];
            float ty1 = charTextureCoordinates[j*4 + 3];
            float xyRatio = charXYratio[j];
            float w = h * charHeights[j] * viewportRatio_ * xyRatio;
            float x0 = x;
            float x1 = x0 + w;
            float y1 = y - charBottoms[j] * h;
            float y0 = y1 + h * charHeights[j];
            float z = 0;
            x += w;

            if(!bboxInitialized) {
                textSize_.set(x0, y0, x0, y0);
                bboxInitialized = true;
            }
            else {
                textSize_.union(x0, y0);
            }
            textSize_.union(x1, y1);

            vertices.put(i*12 + 0, x0);
            vertices.put(i*12 + 1, y0);
            vertices.put(i*12 + 2, z);

            vertices.put(i*12 + 3, x0);
            vertices.put(i*12 + 4, y1);
            vertices.put(i*12 + 5, z);

            vertices.put(i*12 + 6, x1);
            vertices.put(i*12 + 7, y1);
            vertices.put(i*12 + 8, z);

            vertices.put(i*12 + 9, x1);
            vertices.put(i*12 + 10, y0);
            vertices.put(i*12 + 11, z);

            textureCoordinates.put(i*8 + 0, tx0);
            textureCoordinates.put(i*8 + 1, ty0);

            textureCoordinates.put(i*8 + 2, tx0);
            textureCoordinates.put(i*8 + 3, ty1);

            textureCoordinates.put(i*8 + 4, tx1);
            textureCoordinates.put(i*8 + 5, ty1);

            textureCoordinates.put(i*8 + 6, tx1);
            textureCoordinates.put(i*8 + 7, ty0);

            triangles.put(i*6 + 0, (short)(i*4 + 0));
            triangles.put(i*6 + 1, (short)(i*4 + 1));
            triangles.put(i*6 + 2, (short)(i*4 + 2));

            triangles.put(i*6 + 3, (short)(i*4 + 0));
            triangles.put(i*6 + 4, (short)(i*4 + 2));
            triangles.put(i*6 + 5, (short)(i*4 + 3));
        }
    }

    public void draw(Vector3 pos, String text, float h, TextAlign alignH, TextAlign alignV) {
        if(texture_ == null)
            throw new RuntimeException("Error creating font texture.");

        if(!createTextBuffers(text)) {
            return;
        }

        fillTextBuffers(text, h);

        switch(alignH) {
            case Left:
                pos_.setx(pos.x() - textSize_.left);
                break;
            case Right:
                pos_.setx(pos.x() - textSize_.right);
                break;
            case Center:
                pos_.setx(pos.x() - textSize_.centerX());
                break;
        }

        switch(alignV) {
            case Top:
                pos_.sety(pos.y() - textSize_.top);
                break;
            case Bottom:
                pos_.sety(pos.y() - textSize_.bottom);
                break;
            case Center:
                pos_.sety(pos.y() - textSize_.centerY());
                break;
        }

        int indicesN = 2 * INDICES_PER_TRI * text.length();

        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_DEPTH_WRITEMASK);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        texture_.apply();

        int textureHandler = GLES20.glGetUniformLocation(program_, "uTexture");
        GLES20.glUniform1i(textureHandler, 0);

        GLES20.glUseProgram(program_);

        matrixMVP.setIdentity();
        matrixMVP.translate(pos_);
        int mvpHandle = GLES20.glGetUniformLocation(program_, "mvpMatrix");
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, matrixMVP.get(), 0);

        int colorHandle = GLES20.glGetUniformLocation(program_, "vColor");
        GLES20.glUniform4fv(colorHandle, 1, color, 0);

        int positionHandle = GLES20.glGetAttribLocation(program_, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);

        vertices.position(0);
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, vertexStride, vertices);

        int texcoordsHandle = GLES20.glGetAttribLocation(program_, "vTexCoords");
        GLES20.glEnableVertexAttribArray(texcoordsHandle);

        textureCoordinates.rewind();
        GLES20.glVertexAttribPointer(texcoordsHandle, TEXCOORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, texcoordStride, textureCoordinates);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesN, GLES20.GL_UNSIGNED_SHORT, triangles);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    public void drawAllFont() {
        if(texture_ == null)
            throw new RuntimeException("Error creating font texture.");

        if(!createTextBuffers("a")) {
            return;
        }

        {
            float x0 = -1;
            float y0 = 1;
            float x1 = 1;
            float y1 = -1;
            float z = 0;

            vertices.put(0, x0);
            vertices.put(1, y0);
            vertices.put(2, z);

            vertices.put(3, x0);
            vertices.put(4, y1);
            vertices.put(5, z);

            vertices.put(6, x1);
            vertices.put(7, y1);
            vertices.put(8, z);

            vertices.put(9, x1);
            vertices.put(10, y0);
            vertices.put(11, z);

            float tx0 = 0;
            float tx1 = 1;
            float ty0 = 0;
            float ty1 = 1;

            textureCoordinates.put(0, tx0);
            textureCoordinates.put(1, ty0);

            textureCoordinates.put(2, tx0);
            textureCoordinates.put(3, ty1);

            textureCoordinates.put(4, tx1);
            textureCoordinates.put(5, ty1);

            textureCoordinates.put(6, tx1);
            textureCoordinates.put(7, ty0);

            triangles.put(0, (short)(0));
            triangles.put(1, (short)(1));
            triangles.put(2, (short)(2));

            triangles.put(3, (short)(0));
            triangles.put(4, (short)(2));
            triangles.put(5, (short)(3));
        }

        int indicesN = 2 * INDICES_PER_TRI;

        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_DEPTH_WRITEMASK);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        texture_.apply();

        int textureHandler = GLES20.glGetUniformLocation(program_, "uTexture");
        GLES20.glUniform1i(textureHandler, 0);

        GLES20.glUseProgram(program_);

        matrixMVP.setIdentity();
        int mvpHandle = GLES20.glGetUniformLocation(program_, "mvpMatrix");
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, matrixMVP.get(), 0);

        int colorHandle = GLES20.glGetUniformLocation(program_, "vColor");
        GLES20.glUniform4fv(colorHandle, 1, color, 0);

        int positionHandle = GLES20.glGetAttribLocation(program_, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);

        vertices.position(0);
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, vertexStride, vertices);

        int texcoordsHandle = GLES20.glGetAttribLocation(program_, "vTexCoords");
        GLES20.glEnableVertexAttribArray(texcoordsHandle);

        textureCoordinates.rewind();
        GLES20.glVertexAttribPointer(texcoordsHandle, TEXCOORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, texcoordStride, textureCoordinates);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indicesN, GLES20.GL_UNSIGNED_SHORT, triangles);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }
}
