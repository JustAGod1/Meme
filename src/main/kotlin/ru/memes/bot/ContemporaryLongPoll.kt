package ru.memes.bot

import com.google.gson.JsonObject
import com.vk.api.sdk.callback.longpoll.CallbackApiLongPoll
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.GroupActor
import java.net.URL

class ContemporaryLongPoll(client: VkApiClient, actor: GroupActor, val listener: (String, Int) -> Unit)
    : CallbackApiLongPoll(client, actor) {

    override fun parse(event: JsonObject): Boolean {
        val obj = event["object"].asJsonObject ?: return true
        val peerId = obj["peer_id"]?.asInt ?: return true
        val fromId = obj["from_id"]?.asInt ?: return true
        var text = obj["text"]?.asString
        obj["attachments"]?.asJsonArray?.forEach { attachment ->
            val attachmentObj = attachment.asJsonObject
            val type = attachmentObj["type"].asString
            if (type != "doc") return@forEach
            val doc = attachmentObj["doc"].asJsonObject
            val ext = doc["ext"].asString
            if (ext != "commands" && ext != "comms") return@forEach
            text += "\n" + URL(doc["url"].asString).readText()
        }
        if (!text.isNullOrEmpty() && BotCore.groupActor.id != fromId) {
            println("""
                            Peer Id: $peerId
                            From Id: $fromId
                        """.trimIndent())
            listener(text, peerId)
        }

        return true
    }
}