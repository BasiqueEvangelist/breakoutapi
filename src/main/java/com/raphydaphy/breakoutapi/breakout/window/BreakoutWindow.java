package com.raphydaphy.breakoutapi.breakout.window;

import com.mojang.blaze3d.systems.RenderSystem;
import com.raphydaphy.breakoutapi.BreakoutAPI;
import com.raphydaphy.breakoutapi.breakout.window.callback.BreakoutWindowCallbackKeeper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.client.util.Monitor;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.liquidengine.legui.system.context.CallbackKeeper;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class BreakoutWindow {
  private final long handle;
  protected MinecraftClient client;

  private int x, y;
  private int width = 300;
  private int height = 720;
  private int framebufferWidth;
  private int framebufferHeight;

  public final BreakoutWindowCallbackKeeper keeper;

  /***
   * Creates a BreakoutWindow on the same monitor as the main game
   ***/
  public BreakoutWindow(String title, int width, int height) {
    this(title, width, height, null);
  }

  /**
   * Creates a BreakoutWindow
   *
   * @param title The title of the window
   * @param width The width of the window
   * @param height The height of the window
   * @param monitor The monitor which the window should display on.
   *                If set to null, the primary monitor will be used
   */
  public BreakoutWindow(String title, int width, int height, @Nullable Monitor monitor) {
    this.client = MinecraftClient.getInstance();
    this.width = width;
    this.height = height;

    this.handle = GLFW.glfwCreateWindow(this.width, this.height, title, monitor == null ? 0L : monitor.getHandle(),this.client.getWindow().getHandle());

    GLFW.glfwMakeContextCurrent(this.handle);
    this.updateFramebufferSize();

    this.keeper = new BreakoutWindowCallbackKeeper();
    CallbackKeeper.registerCallbacks(this.getHandle(), this.keeper);

    this.keeper.getChainFramebufferSizeCallback().add(this::onFramebufferSizeChanged);
    this.keeper.getChainWindowSizeCallback().add(this::onWindowSizeChanged);
    this.keeper.getChainWindowPosCallback().add(this::onWindowPosChanged);
  }

  public void setPos(int x, int y) {
    GLFW.glfwSetWindowPos(this.handle, x, y);
    this.x = x;
    this.y = y;
  }

  /***
   * Sets the window position relative to the main Minecraft window
   *
   * @param offsetX The window offset on the X axis
   * @param offsetY The window offset on the Y axis
   ***/
  public void setRelativePos(int offsetX, int offsetY) {
    Window main = this.client.getWindow();

    int x = main.getX() + offsetX;
    int y = main.getY() + offsetY;

    this.setPos(x, y);
  }

  public void setSize(int width, int height) {
    GLFW.glfwSetWindowSize(this.handle, width, height);
  }

  private void onWindowPosChanged(long window, int x, int y) {
    if (window == this.handle){
      this.x = x;
      this.y = y;
    }
  }

  private void onWindowSizeChanged(long window, int width, int height) {
    if (window == this.handle) {
      this.width = width;
      this.height = height;
    }
  }

  private void onFramebufferSizeChanged(long window, int width, int height) {
    if (window == this.handle) {
      int oldWidth = this.getFramebufferWidth();
      int oldHeight = this.getFramebufferHeight();
      if (width != 0 && height != 0) {
        this.framebufferWidth = width;
        this.framebufferHeight = height;
        if (this.getFramebufferWidth() != oldWidth || this.getFramebufferHeight() != oldHeight) {
          this.keeper.getChainResolutionChangedCallback().invoke(width, height);
        }
      }
    }
  }

  private void updateFramebufferSize() {
    int[] framebufferWidth = new int[1];
    int[] framebufferHeight = new int[1];
    GLFW.glfwGetFramebufferSize(this.handle, framebufferWidth, framebufferHeight);
    this.framebufferWidth = framebufferWidth[0];
    this.framebufferHeight = framebufferHeight[0];
  }

  public void swapBuffers() {
    RenderSystem.flipFrame(this.handle);
  }

  public void setIcon(Identifier icon16, Identifier icon32) {
    RenderSystem.assertThread(RenderSystem::isInInitPhase);

    try {
      InputStream icon16Stream = this.client.getResourcePackDownloader().getPack().open(ResourceType.CLIENT_RESOURCES, icon16);
      InputStream icon32Stream = this.client.getResourcePackDownloader().getPack().open(ResourceType.CLIENT_RESOURCES, icon32);

      MemoryStack memoryStack = MemoryStack.stackPush();
      Throwable var4 = null;

      try {
        if (icon16Stream == null) {
          throw new FileNotFoundException(icon16.toString());
        }

        if (icon32Stream == null) {
          throw new FileNotFoundException(icon32.toString());
        }

        IntBuffer intBuffer = memoryStack.mallocInt(1);
        IntBuffer intBuffer2 = memoryStack.mallocInt(1);
        IntBuffer intBuffer3 = memoryStack.mallocInt(1);
        GLFWImage.Buffer buffer = GLFWImage.mallocStack(2, memoryStack);
        ByteBuffer byteBuffer = this.readImage(icon16Stream, intBuffer, intBuffer2, intBuffer3);
        if (byteBuffer == null) {
          throw new IllegalStateException("Could not load icon: " + STBImage.stbi_failure_reason());
        }

        buffer.position(0);
        buffer.width(intBuffer.get(0));
        buffer.height(intBuffer2.get(0));
        buffer.pixels(byteBuffer);
        ByteBuffer byteBuffer2 = this.readImage(icon32Stream, intBuffer, intBuffer2, intBuffer3);
        if (byteBuffer2 == null) {
          throw new IllegalStateException("Could not load icon: " + STBImage.stbi_failure_reason());
        }

        buffer.position(1);
        buffer.width(intBuffer.get(0));
        buffer.height(intBuffer2.get(0));
        buffer.pixels(byteBuffer2);
        buffer.position(0);
        GLFW.glfwSetWindowIcon(this.handle, buffer);
        STBImage.stbi_image_free(byteBuffer);
        STBImage.stbi_image_free(byteBuffer2);
      } catch (Throwable var19) {
        var4 = var19;
        throw var19;
      } finally {
        if (memoryStack != null) {
          if (var4 != null) {
            try {
              memoryStack.close();
            } catch (Throwable var18) {
              var4.addSuppressed(var18);
            }
          } else {
            memoryStack.close();
          }
        }

      }
    } catch (IOException e) {
      BreakoutAPI.LOGGER.error("Couldn't set icon", e);
    }

  }

  @Nullable
  private ByteBuffer readImage(InputStream in, IntBuffer x, IntBuffer y, IntBuffer channels) throws IOException {
    RenderSystem.assertThread(RenderSystem::isInInitPhase);
    ByteBuffer byteBuffer = null;

    ByteBuffer var6;
    try {
      byteBuffer = TextureUtil.readAllToByteBuffer(in);
      byteBuffer.rewind();
      var6 = STBImage.stbi_load_from_memory(byteBuffer, x, y, channels, 0);
    } finally {
      if (byteBuffer != null) {
        MemoryUtil.memFree(byteBuffer);
      }

    }

    return var6;
  }

  public long getHandle() {
    return this.handle;
  }

  @Nullable
  public Monitor getMonitor() {
    return BreakoutMonitorTracker.getMonitor(this);
  }

  public int getX() {
    return this.x;
  }

  public int getY() {
    return this.y;
  }

  public int getWidth() {
    return this.width;
  }

  public int getHeight() {
    return this.height;
  }

  public int getFramebufferWidth() {
    return this.framebufferWidth;
  }

  public int getFramebufferHeight() {
    return this.framebufferHeight;
  }

  public boolean shouldClose() {
    return GLFW.glfwWindowShouldClose(handle);
  }

  public void destroy() {
    GLFW.glfwDestroyWindow(this.handle);
  }
}
