package imgui.windowsIme

import glm_.L
import imgui.MINECRAFT_BEHAVIORS
import kool.BYTES
import org.lwjgl.system.JNI
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memGetLong
import org.lwjgl.system.MemoryUtil.memPutLong
import org.lwjgl.system.Platform
import org.lwjgl.system.SharedLibrary
import org.lwjgl.system.windows.WindowsLibrary
import uno.kotlin.HWND
import java.nio.ByteBuffer

object imm {

    val lib: SharedLibrary = WindowsLibrary("Imm32")

    val ImmCreateContext = lib.getFunctionAddress("ImmCreateContext")
    val ImmGetContext = lib.getFunctionAddress("ImmGetContext")
    val ImmSetCandidateWindow = lib.getFunctionAddress("ImmSetCandidateWindow")
    val ImmSetCompositionWindow = lib.getFunctionAddress("ImmSetCompositionWindow")
    val ImmReleaseContext = lib.getFunctionAddress("ImmReleaseContext")

    fun getContext(hwnd: HWND): Long {
        return if (Platform.get() == Platform.WINDOWS && MINECRAFT_BEHAVIORS) {
            JNI.callP(ImmCreateContext)
        } else {
            JNI.callPP(hwnd.L, ImmGetContext)
        }
    }

    fun setCandidateWindow(himc: HIMC, candForm: CANDIDATEFORM) = JNI.callPPI(himc.L, candForm.adr, ImmSetCandidateWindow)
    fun setCompositionWindow(himc: HIMC, compForm: COMPOSITIONFORM) = JNI.callPPI(himc.L, compForm.adr, ImmSetCompositionWindow)
    fun releaseContext(hwnd: HWND, himc: HIMC) = JNI.callPPI(hwnd.L, himc.L, ImmReleaseContext)

    // bit field for IMC_SETCOMPOSITIONWINDOW, IMC_SETCANDIDATEWINDOW
    val CFS_DEFAULT = 0x0000
    val CFS_RECT = 0x0001
    val CFS_POINT = 0x0002
    val CFS_FORCE_POSITION = 0x0020
    val CFS_CANDIDATEPOS = 0x0040
    val CFS_EXCLUDE = 0x0080
}


// TODO -> uno
@JvmInline
value class HIMC(val L: Long)

@JvmInline
value class DWORD(val L: Long) {
    companion object {
        val BYTES get() = Long.BYTES
    }
}

/**
 * typedef struct tagCANDIDATEFORM {
 *     DWORD dwIndex;
 *     DWORD dwStyle;
 *     POINT ptCurrentPos;
 *     RECT  rcArea;
 * } CANDIDATEFORM, *PCANDIDATEFORM;
 */
class CANDIDATEFORM(val adr: Long) {

    val buffer: ByteBuffer = MemoryUtil.memByteBuffer(adr, size)

    constructor() : this(MemoryUtil.nmemAlloc(COMPOSITIONFORM.size.L))

    var dwIndex: DWORD
        get() = DWORD(memGetLong(adr + ofs.dwIndex))
        set(value) = memPutLong(adr + ofs.dwIndex, value.L)

    var dwStyle: DWORD
        get() = DWORD(memGetLong(adr + ofs.dwStyle))
        set(value) = memPutLong(adr + ofs.dwStyle, value.L)

    var ptCurrentPos: POINT
        get() = POINT(adr + ofs.ptCurrentPos)
        set(value) = value.to(adr + ofs.ptCurrentPos)

    var rcArea: RECT
        get() = RECT(adr + ofs.rcArea)
        set(value) = value.to(adr + ofs.rcArea)

    companion object {
        val size = 2 * Int.BYTES + POINT.size + RECT.size
    }

    object ofs {
        val dwIndex = 0
        val dwStyle = dwIndex + DWORD.BYTES
        val ptCurrentPos = dwStyle + DWORD.BYTES
        val rcArea = ptCurrentPos + POINT.size
    }
}

/**
 * typedef struct tagCOMPOSITIONFORM {
 *      DWORD dwStyle;
 *      POINT ptCurrentPos;
 *      RECT  rcArea;
 * } COMPOSITIONFORM
 */
class COMPOSITIONFORM(val adr: Long) {

    val buffer: ByteBuffer = MemoryUtil.memByteBuffer(adr, size)

    constructor() : this(MemoryUtil.nmemAlloc(size.L))

    var dwStyle: DWORD
        get() = DWORD(memGetLong(adr + ofs.dwStyle))
        set(value) = memPutLong(adr + ofs.dwStyle, value.L)

    var ptCurrentPos: POINT
        get() = POINT(adr + ofs.ptCurrentPos)
        set(value) = value.to(adr + ofs.ptCurrentPos)

    var rcArea: RECT
        get() = RECT(adr + ofs.rcArea)
        set(value) = value.to(adr + ofs.rcArea)

    fun free() = MemoryUtil.nmemFree(adr)

    companion object {
        val size = 2 * Int.BYTES + POINT.size + RECT.size
    }

    object ofs {
        val dwStyle = 0
        val ptCurrentPos = dwStyle + POINT.size
        val rcArea = ptCurrentPos + RECT.size
    }
}

/** typedef struct tagPOINT {
 *      LONG x;
 *      LONG y;
 *  } POINT, *PPOINT;
 */
class POINT constructor(val adr: Long) {

    val buffer: ByteBuffer = MemoryUtil.memByteBuffer(adr, size)

    var x: Long
        get() = MemoryUtil.memGetLong(adr + ofs.x)
        set(value) = MemoryUtil.memPutLong(adr + ofs.x, value)
    var y: Long
        get() = MemoryUtil.memGetLong(adr + ofs.y)
        set(value) = MemoryUtil.memPutLong(adr + ofs.y, value)

    constructor() : this(MemoryUtil.nmemAlloc(size.L))

    fun to(adr: Long) {
        MemoryUtil.memPutLong(adr + ofs.x, x)
        MemoryUtil.memPutLong(adr + ofs.y, y)
    }

    companion object {
        val size = 2 * Long.BYTES
    }

    object ofs {
        val x = 0
        val y = x + Long.BYTES
    }
}

/**
 * typedef struct _RECT {
 *     LONG left;
 *     LONG top;
 *     LONG right;
 *     LONG bottom;
 * } RECT, *PRECT;
 */
class RECT(val adr: Long) {

    val buffer: ByteBuffer = MemoryUtil.memByteBuffer(adr, size)

    var left: Long
        get() = MemoryUtil.memGetLong(adr + ofs.left)
        set(value) = MemoryUtil.memPutLong(adr + ofs.left, value)
    var top: Long
        get() = MemoryUtil.memGetLong(adr + ofs.top)
        set(value) = MemoryUtil.memPutLong(adr + ofs.top, value)
    var right: Long
        get() = MemoryUtil.memGetLong(adr + ofs.right)
        set(value) = MemoryUtil.memPutLong(adr + ofs.right, value)
    var bottom: Long
        get() = MemoryUtil.memGetLong(adr + ofs.bottom)
        set(value) = MemoryUtil.memPutLong(adr + ofs.bottom, value)

    constructor() : this(MemoryUtil.nmemAlloc(size.L))

    fun to(adr: Long) {
        MemoryUtil.memPutLong(adr + ofs.left, left)
        MemoryUtil.memPutLong(adr + ofs.top, top)
        MemoryUtil.memPutLong(adr + ofs.right, right)
        MemoryUtil.memPutLong(adr + ofs.bottom, bottom)
    }

    companion object {
        val size = 4 * Long.BYTES
    }

    object ofs {
        val left = 0
        val top = left + Long.BYTES
        val right = top + Long.BYTES
        val bottom = right + Long.BYTES
    }
}
