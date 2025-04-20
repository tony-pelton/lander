package com.dsrts.lander;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;

public class STBFontRenderer {
    private final ByteBuffer fontBuffer;
    private final ByteBuffer bitmap;
    private final int texId;
    private final int bW = 512, bH = 512;
    private final STBTTBakedChar.Buffer cdata;

    public STBFontRenderer(String fontResource, float pixelHeight) {
        try {
            fontBuffer = ioResourceToByteBuffer(fontResource, 160 * 1024);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        bitmap = BufferUtils.createByteBuffer(bW * bH);
        cdata = STBTTBakedChar.malloc(96);
        STBTruetype.stbtt_BakeFontBitmap(fontBuffer, pixelHeight, bitmap, bW, bH, 32, cdata);
        texId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_ALPHA, bW, bH, 0, GL11.GL_ALPHA, GL11.GL_UNSIGNED_BYTE, bitmap);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
    }

    public void drawText(String text, float x, float y, float sx, float sy) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer xb = stack.floats(x);
            FloatBuffer yb = stack.floats(y);
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n') {
                    xb.put(0, x);
                    yb.put(0, yb.get(0) + 24 * sy);
                    continue;
                }
                STBTruetype.stbtt_GetBakedQuad(cdata, bW, bH, c - 32, xb, yb, q, true);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glTexCoord2f(q.s0(), q.t0()); GL11.glVertex2f(q.x0() * sx, q.y0() * sy);
                GL11.glTexCoord2f(q.s1(), q.t0()); GL11.glVertex2f(q.x1() * sx, q.y0() * sy);
                GL11.glTexCoord2f(q.s1(), q.t1()); GL11.glVertex2f(q.x1() * sx, q.y1() * sy);
                GL11.glTexCoord2f(q.s0(), q.t1()); GL11.glVertex2f(q.x0() * sx, q.y1() * sy);
                GL11.glEnd();
            }
        }
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private static ByteBuffer ioResourceToByteBuffer(String resource, int bufferSize) throws IOException {
        ByteBuffer buffer;
        try (InputStream source = STBFontRenderer.class.getResourceAsStream(resource)) {
            if (source == null) throw new IOException("Resource not found: " + resource);
            buffer = BufferUtils.createByteBuffer(bufferSize);
            var rbc = Channels.newChannel(source);
            while (true) {
                int bytes = rbc.read(buffer);
                if (bytes == -1) break;
                if (buffer.remaining() == 0)
                    buffer = resizeBuffer(buffer, buffer.capacity() * 2);
            }
            buffer.flip();
        }
        return buffer;
    }
    private static ByteBuffer resizeBuffer(ByteBuffer buffer, int newCapacity) {
        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }
}
