package `in`.gym.trak.studio

import `in`.gym.trak.studio.getPlatform

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}
