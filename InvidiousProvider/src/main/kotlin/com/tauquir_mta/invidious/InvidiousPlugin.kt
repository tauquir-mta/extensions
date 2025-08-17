package com.tauquir_mta.invidious // CORRECT: Uses UNDERSCORE

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class InvidiousPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(InvidiousProvider())
    }
}