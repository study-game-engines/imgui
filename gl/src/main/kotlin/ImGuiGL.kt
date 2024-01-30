package imgui.gl

import imgui.internal.DrawData

interface ImGuiGL {
    fun shutdown()
    fun newFrame()
    fun renderDrawData(drawData: DrawData)
    fun createFontsTexture(): Boolean
    fun destroyFontsTexture()
    fun createDeviceObjects(): Boolean
    fun destroyDeviceObjects()
}