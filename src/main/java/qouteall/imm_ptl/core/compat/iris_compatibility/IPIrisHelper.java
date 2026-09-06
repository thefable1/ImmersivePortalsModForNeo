package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL43C;

import static org.lwjgl.opengl.GL11.*;

public class IPIrisHelper {
    
    public static void copyDepthStencil(
        RenderTarget from, RenderTarget to,
        boolean copyDepth, boolean copyStencil
    ) {
        from.unbindWrite();
        
        int mask = 0;
        
        if (copyDepth) {
            if (copyStencil) {
                mask = GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT;
            }
            else {
                mask = GL_DEPTH_BUFFER_BIT;
            }
        }
        else {
            if (copyStencil) {
                mask = GL_STENCIL_BUFFER_BIT;
            }
            else {
                throw new RuntimeException();
            }
        }
        
        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, from.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, to.frameBufferId);
        
        GL30.glBlitFramebuffer(
            0, 0, from.width, from.height,
            0, 0, to.width, to.height,
            mask, GL_NEAREST
        );
        
        from.unbindWrite();
    }
    
    private static boolean isCopyImageSubDataSupported() {
        return GL.getCapabilities().glCopyImageSubData != 0;
    }
    
    public static void newCopyDepthStencil(
        RenderTarget from, RenderTarget to
    ) {
        GL43C.glCopyImageSubData(
            from.getDepthTextureId(),
            GL43C.GL_TEXTURE_2D,
            0,
            0,
            0,
            0,
            to.getDepthTextureId(),
            GL43C.GL_TEXTURE_2D,
            0,
            0,
            0,
            0,
            from.width,
            from.height,
            1
        );
    }
    
    public static void copyColor(
        RenderTarget from, RenderTarget to
    ) {
        GL43C.glCopyImageSubData(
            from.getColorTextureId(),
            GL43C.GL_TEXTURE_2D,
            0,
            0,
            0,
            0,
            to.getColorTextureId(),
            GL43C.GL_TEXTURE_2D,
            0,
            0,
            0,
            0,
            from.width,
            from.height,
            1
        );
    }

    // glBlitFramebuffer-based alternative to copyColor()/newCopyDepthStencil() above.
    // glCopyImageSubData requires the source and destination textures to have identical
    // internal formats, which breaks whenever Iris reformats the main render target
    // (e.g. for shader-required precision/stencil); blitting is far more tolerant of
    // format differences between the two framebuffers.
    public static void blit(
        RenderTarget from, RenderTarget to,
        boolean copyColor, boolean copyDepth, boolean copyStencil
    ) {
        int mask = 0;
        if (copyColor) mask |= GL_COLOR_BUFFER_BIT;
        if (copyDepth) mask |= GL_DEPTH_BUFFER_BIT;
        if (copyStencil) mask |= GL_STENCIL_BUFFER_BIT;

        from.unbindWrite();

        GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, from.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, to.frameBufferId);

        GL30.glBlitFramebuffer(
            0, 0, from.width, from.height,
            0, 0, to.width, to.height,
            mask, GL_NEAREST
        );

        from.unbindWrite();
    }

}
