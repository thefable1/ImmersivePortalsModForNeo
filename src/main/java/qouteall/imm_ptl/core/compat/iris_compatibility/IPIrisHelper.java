package qouteall.imm_ptl.core.compat.iris_compatibility;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL43C;
import qouteall.imm_ptl.core.CHelper;

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

    // Diagnostic-only: prints the actual GPU internal format of each texture involved
    // in the main-target -> deferred-buffer copy, so a format mismatch (which shows up
    // as GL_INVALID_OPERATION on both glCopyImageSubData and glBlitFramebuffer) can be
    // identified directly instead of guessed at.
    public static void logFormatDiagnostics(RenderTarget from, RenderTarget to) {
        int mainDepthFormat = queryTextureInternalFormat(from.getDepthTextureId());
        int mainColorFormat = queryTextureInternalFormat(from.getColorTextureId());
        int secondaryDepthFormat = queryTextureInternalFormat(to.getDepthTextureId());
        int secondaryColorFormat = queryTextureInternalFormat(to.getColorTextureId());

        CHelper.printChat(String.format(
            "[ImmPtl Debug] main target depth=0x%X color=0x%X | secondary buffer depth=0x%X color=0x%X",
            mainDepthFormat, mainColorFormat, secondaryDepthFormat, secondaryColorFormat
        ));
    }

    private static int queryTextureInternalFormat(int textureId) {
        int previouslyBound = glGetInteger(GL_TEXTURE_BINDING_2D);
        glBindTexture(GL_TEXTURE_2D, textureId);
        int[] out = new int[1];
        glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_INTERNAL_FORMAT, out);
        glBindTexture(GL_TEXTURE_2D, previouslyBound);
        return out[0];
    }

}
