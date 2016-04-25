/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package framework;

import java.io.UnsupportedEncodingException;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>BufferUtils</code> is a helper class for generating nio buffers from
 * jME data classes such as Vectors and ColorRGBA.
 * 
 * @author Joshua Slack
 * @version $Id: BufferUtils.java,v 1.16 2007/10/29 16:56:18 nca Exp $
 */
public final class BufferUtils {

    private static boolean trackDirectMemory = false;
    private static ReferenceQueue<Buffer> removeCollected = new ReferenceQueue<Buffer>();
    private static ConcurrentHashMap<BufferInfo, BufferInfo> trackedBuffers = new ConcurrentHashMap<BufferInfo, BufferInfo>();
    static ClearReferences cleanupthread;

    /**
     * Set it to true if you want to enable direct memory tracking for debugging purpose.
     * Default is false.
     * To print direct memory usage use BufferUtils.printCurrentDirectMemory(StringBuilder store);
     * @param enabled 
     */
    public static void setTrackDirectMemoryEnabled(boolean enabled) {
        trackDirectMemory = enabled;
    }

    /**
     * Creates a clone of the given buffer. The clone's capacity is 
     * equal to the given buffer's limit.
     * 
     * @param buf The buffer to clone
     * @return The cloned buffer
     */
    public static Buffer clone(Buffer buf) {
        if (buf instanceof FloatBuffer) {
            return clone((FloatBuffer) buf);
        } else if (buf instanceof ShortBuffer) {
            return clone((ShortBuffer) buf);
        } else if (buf instanceof ByteBuffer) {
            return clone((ByteBuffer) buf);
        } else if (buf instanceof IntBuffer) {
            return clone((IntBuffer) buf);
        } else if (buf instanceof DoubleBuffer) {
            return clone((DoubleBuffer) buf);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static void onBufferAllocated(Buffer buffer) {
        /**
         * StackTraceElement[] stackTrace = new Throwable().getStackTrace(); int
         * initialIndex = 0;
         * 
         * for (int i = 0; i < stackTrace.length; i++){ if
         * (!stackTrace[i].getClassName().equals(BufferUtils.class.getName())){
         * initialIndex = i; break; } }
         * 
         * int allocated = buffer.capacity(); int size = 0;
         * 
         * if (buffer instanceof FloatBuffer){ size = 4; }else if (buffer
         * instanceof ShortBuffer){ size = 2; }else if (buffer instanceof
         * ByteBuffer){ size = 1; }else if (buffer instanceof IntBuffer){ size =
         * 4; }else if (buffer instanceof DoubleBuffer){ size = 8; }
         * 
         * allocated *= size;
         * 
         * for (int i = initialIndex; i < stackTrace.length; i++){
         * StackTraceElement element = stackTrace[i]; if
         * (element.getClassName().startsWith("java")){ break; }
         * 
         * try { Class clazz = Class.forName(element.getClassName()); if (i ==
         * initialIndex){
         * System.out.println(clazz.getSimpleName()+"."+element.getMethodName
         * ()+"():" + element.getLineNumber() + " allocated " + allocated);
         * }else{ System.out.println(" at " +
         * clazz.getSimpleName()+"."+element.getMethodName()+"()"); } } catch
         * (ClassNotFoundException ex) { } }
         */
        if (BufferUtils.trackDirectMemory) {

            if (BufferUtils.cleanupthread == null) {
                BufferUtils.cleanupthread = new ClearReferences();
                BufferUtils.cleanupthread.start();
            }
            if (buffer instanceof ByteBuffer) {
                BufferInfo info = new BufferInfo(ByteBuffer.class, buffer.capacity(), buffer, BufferUtils.removeCollected);
                BufferUtils.trackedBuffers.put(info, info);
            } else if (buffer instanceof FloatBuffer) {
                BufferInfo info = new BufferInfo(FloatBuffer.class, buffer.capacity() * 4, buffer, BufferUtils.removeCollected);
                BufferUtils.trackedBuffers.put(info, info);
            } else if (buffer instanceof IntBuffer) {
                BufferInfo info = new BufferInfo(IntBuffer.class, buffer.capacity() * 4, buffer, BufferUtils.removeCollected);
                BufferUtils.trackedBuffers.put(info, info);
            } else if (buffer instanceof ShortBuffer) {
                BufferInfo info = new BufferInfo(ShortBuffer.class, buffer.capacity() * 2, buffer, BufferUtils.removeCollected);
                BufferUtils.trackedBuffers.put(info, info);
            } else if (buffer instanceof DoubleBuffer) {
                BufferInfo info = new BufferInfo(DoubleBuffer.class, buffer.capacity() * 8, buffer, BufferUtils.removeCollected);
                BufferUtils.trackedBuffers.put(info, info);
            }

        }
    }
    
    public static void printCurrentDirectMemory(StringBuilder store) {
        long totalHeld = 0;
        long heapMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        boolean printStout = store == null;
        if (store == null) {
            store = new StringBuilder();
        }
        if (trackDirectMemory) {
            // make a new set to hold the keys to prevent concurrency issues.
            int fBufs = 0, bBufs = 0, iBufs = 0, sBufs = 0, dBufs = 0;
            int fBufsM = 0, bBufsM = 0, iBufsM = 0, sBufsM = 0, dBufsM = 0;
            for (BufferInfo b : BufferUtils.trackedBuffers.values()) {
                if (b.type == ByteBuffer.class) {
                    totalHeld += b.size;
                    bBufsM += b.size;
                    bBufs++;
                } else if (b.type == FloatBuffer.class) {
                    totalHeld += b.size;
                    fBufsM += b.size;
                    fBufs++;
                } else if (b.type == IntBuffer.class) {
                    totalHeld += b.size;
                    iBufsM += b.size;
                    iBufs++;
                } else if (b.type == ShortBuffer.class) {
                    totalHeld += b.size;
                    sBufsM += b.size;
                    sBufs++;
                } else if (b.type == DoubleBuffer.class) {
                    totalHeld += b.size;
                    dBufsM += b.size;
                    dBufs++;
                }
            }

            store.append("Existing buffers: ").append(BufferUtils.trackedBuffers.size()).append("\n");
            store.append("(b: ").append(bBufs).append("  f: ").append(fBufs).append("  i: ").append(iBufs).append("  s: ").append(sBufs).append("  d: ").append(dBufs).append(")").append("\n");
            store.append("Total   heap memory held: ").append(heapMem / 1024).append("kb\n");
            store.append("Total direct memory held: ").append(totalHeld / 1024).append("kb\n");
            store.append("(b: ").append(bBufsM / 1024).append("kb  f: ").append(fBufsM / 1024).append("kb  i: ").append(iBufsM / 1024).append("kb  s: ").append(sBufsM / 1024).append("kb  d: ").append(dBufsM / 1024).append("kb)").append("\n");
        } else {
            store.append("Total   heap memory held: ").append(heapMem / 1024).append("kb\n");
            store.append("Only heap memory available, if you want to monitor direct memory use BufferUtils.setTrackDirectMemoryEnabled(true) during initialization.").append("\n");
        }
        if (printStout) {
            System.out.println(store.toString());
        }
    }
    private static final AtomicBoolean loadedMethods = new AtomicBoolean(false);
    private static Method cleanerMethod = null;
    private static Method cleanMethod = null;
    private static Method viewedBufferMethod = null;
    private static Method freeMethod = null;

    private static Method loadMethod(String className, String methodName) {
        try {
            Method method = Class.forName(className).getMethod(methodName);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException ex) {
            return null; // the method was not found
        }
        // setAccessible not allowed by security policy
        // the direct buffer implementation was not found
        
    }
    
    
    //// -- GENERAL BYTE ROUTINES -- ////
    /**
     * Create a new ByteBuffer of the specified size.
     * 
     * @param size
     *            required number of ints to store.
     * @return the new IntBuffer
     */
    public static ByteBuffer createByteBuffer(int size) {
        ByteBuffer buf = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        buf.clear();
        onBufferAllocated(buf);
        return buf;
    }

    private static void loadCleanerMethods() {
        // If its already true, exit, if not, set it to true.
        if (BufferUtils.loadedMethods.getAndSet(true)) {
            return;
        }
        // This could potentially be called many times if used from multiple
        // threads
        synchronized (BufferUtils.loadedMethods) {
            // Oracle JRE / OpenJDK
            cleanerMethod = loadMethod("sun.nio.ch.DirectBuffer", "cleaner");
            cleanMethod = loadMethod("sun.misc.Cleaner", "clean");
            viewedBufferMethod = loadMethod("sun.nio.ch.DirectBuffer", "viewedBuffer");
            if (viewedBufferMethod == null) {
                // They changed the name in Java 7 (???)
                viewedBufferMethod = loadMethod("sun.nio.ch.DirectBuffer", "attachment");
            }

            // Apache Harmony
            ByteBuffer bb = BufferUtils.createByteBuffer(1);
            Class<?> clazz = bb.getClass();
            try {
                freeMethod = clazz.getMethod("free");
            } catch (NoSuchMethodException | SecurityException ex) {
            }
        }
    }

    /**
     * Direct buffers are garbage collected by using a phantom reference and a
     * reference queue. Every once a while, the JVM checks the reference queue and
     * cleans the direct buffers. However, as this doesn't happen
     * immediately after discarding all references to a direct buffer, it's
     * easy to OutOfMemoryError yourself using direct buffers. This function
     * explicitly calls the Cleaner method of a direct buffer.
     * 
     * @param toBeDestroyed
     *          The direct buffer that will be "cleaned". Utilizes reflection.
     * 
     */
    public static void destroyDirectBuffer(Buffer toBeDestroyed) {
        if (!isDirect(toBeDestroyed)) {
            return;
        }

        BufferUtils.loadCleanerMethods();

        try {
            if (freeMethod != null) {
                freeMethod.invoke(toBeDestroyed);
            } else {
                Object cleaner = cleanerMethod.invoke(toBeDestroyed);
                if (cleaner != null) {
                    cleanMethod.invoke(cleaner);
                } else {
                    // Try the alternate approach of getting the viewed buffer first
                    Object viewedBuffer = viewedBufferMethod.invoke(toBeDestroyed);
                    if (viewedBuffer != null) {
                        destroyDirectBuffer((Buffer) viewedBuffer);
                    } else {
                        Logger.getLogger(BufferUtils.class.getName()).log(Level.SEVERE, "Buffer cannot be destroyed: {0}", toBeDestroyed);
                    }
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException ex) {
            Logger.getLogger(BufferUtils.class.getName()).log(Level.SEVERE, "{0}", ex);
        }
    }
    
    /*
     * FIXME when java 1.5 supprt is dropped - replace calls to this method with Buffer.isDirect 
     * 
     * Buffer.isDirect() is only java 6. Java 5 only have this method on Buffer subclasses : 
     * FloatBuffer, IntBuffer, ShortBuffer, ByteBuffer,DoubleBuffer, LongBuffer.   
     * CharBuffer has been excluded as we don't use it.
     * 
     */
    private static boolean isDirect(Buffer buf) {
        if (buf instanceof FloatBuffer) {
            return ((FloatBuffer) buf).isDirect();
        }
        if (buf instanceof IntBuffer) {
            return ((IntBuffer) buf).isDirect();
        }
        if (buf instanceof ShortBuffer) {
            return ((ShortBuffer) buf).isDirect();
        }
        if (buf instanceof ByteBuffer) {
            return ((ByteBuffer) buf).isDirect();
        }
        if (buf instanceof DoubleBuffer) {
            return ((DoubleBuffer) buf).isDirect();
        }
        if (buf instanceof LongBuffer) {
            return ((LongBuffer) buf).isDirect();
        }
        throw new UnsupportedOperationException(" BufferUtils.isDirect was called on " + buf.getClass().getName());
    }

    private static class BufferInfo extends PhantomReference<Buffer> {

        private Class type;
        private int size;

        public BufferInfo(Class type, int size, Buffer referent, ReferenceQueue<? super Buffer> q) {
            super(referent, q);
            this.type = type;
            this.size = size;
        }
    }

    private static class ClearReferences extends Thread {

        ClearReferences() {
            this.setDaemon(true);
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Reference<? extends Buffer> toclean = BufferUtils.removeCollected.remove();
                    BufferUtils.trackedBuffers.remove(toclean);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
