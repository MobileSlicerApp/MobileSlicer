package com.mobileslicer.viewer

import android.opengl.GLES20

internal object ViewerTextureProgram {
    fun create(): TextureProgram {
        val programId = buildProgram(TEXTURE_VERTEX_SHADER, TEXTURE_FRAGMENT_SHADER)
        return TextureProgram(
            programId = programId,
            handles = TextureProgramHandles(
                positionHandle = GLES20.glGetAttribLocation(programId, "aPosition"),
                uvHandle = GLES20.glGetAttribLocation(programId, "aTexCoord"),
                matrixHandle = GLES20.glGetUniformLocation(programId, "uViewProjectionMatrix"),
                textureHandle = GLES20.glGetUniformLocation(programId, "uTexture"),
                alphaHandle = GLES20.glGetUniformLocation(programId, "uAlpha")
            )
        )
    }

    private fun buildProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        return GLES20.glCreateProgram().also { program ->
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val log = GLES20.glGetProgramInfoLog(program)
                GLES20.glDeleteProgram(program)
                error("OpenGL texture program link failed: $log")
            }
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                val log = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                error("OpenGL texture shader compile failed: $log")
            }
        }
    }

    private const val TEXTURE_VERTEX_SHADER = """
        uniform mat4 uViewProjectionMatrix;
        attribute vec3 aPosition;
        attribute vec2 aTexCoord;
        varying vec2 vTexCoord;

        void main() {
            vTexCoord = aTexCoord;
            gl_Position = uViewProjectionMatrix * vec4(aPosition, 1.0);
        }
    """

    private const val TEXTURE_FRAGMENT_SHADER = """
        precision mediump float;
        uniform sampler2D uTexture;
        uniform float uAlpha;
        varying vec2 vTexCoord;

        void main() {
            vec4 sampleColor = texture2D(uTexture, vTexCoord);
            gl_FragColor = vec4(sampleColor.rgb, sampleColor.a * uAlpha);
        }
    """
}
