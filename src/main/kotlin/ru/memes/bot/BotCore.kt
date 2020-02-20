package ru.memes.bot

import com.google.gson.Gson
import com.vk.api.sdk.client.ApiRequest
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.GroupActor
import com.vk.api.sdk.exceptions.ApiException
import com.vk.api.sdk.httpclient.HttpTransportClient
import java.security.SecureRandom

object BotCore {

    var vk = VkApiClient(HttpTransportClient(), Gson(), 1)
    val groupActor = GroupActor(192194845, "b2c9e5449bd5a0d1a3e219549b96a4894fa7961ae2f3f0c551bc67a8ab2b0fee25b6c3b289a065cc7e98c")


    @JvmStatic
    fun main(args: Array<String>) {
        while (true) {
            try {
                ContemporaryLongPoll(vk, groupActor, this::processMessage).run()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Thread.sleep(1000)
        }
    }


    private val usage = """
        This bot generates random sequence of letters 'х' and 'а' 
        Probably it will make your life easier
        
        Usage <grasp>, <meme_lvl>
        
        Grasp:
        At this momemnt bot supports only two states of this flag: 0 or 1
        0 means that you haven't understood the meme so bot will generate "Не понял"
        
        Meme lvl:
        Just rate the meme that you've just received. It'll be parsed into integer so don't type too big values.
    """.trimIndent()

    private fun processMessage(content: String, peer: Int) {
        val splitted = content.split(",").map { it.trim() }

        if (splitted.size < 2) {
            sendMessage(peer, usage)
            return
        }

        val grasp = splitted[0].toIntOrNull()
        if (grasp == null) {
            sendMessage(peer, usage)
            return
        }
        val lvl = splitted[1].toIntOrNull()
        if (lvl == null) {
            sendMessage(peer, usage)
            return
        }


        sendMessage(peer, generateSequence(grasp, lvl))

    }


    private fun generateSequence(grasp: Int, lvl: Int): String {
        if (grasp < 1) return "Не понял"

        val rand = SecureRandom()
        val sb = StringBuilder()

        repeat((lvl + 1) * 2) {
            if (rand.nextInt(2) == 0) sb.append('х') else sb.append('а')
        }

        return sb.toString()
    }


    fun <T> ApiRequest<T>.executeWithWaitingRetries(): T {
        var exception: ApiException? = null
        for (i in 0 until 5) {
            try {
                return execute()
            } catch (e: ApiException) {
                Thread.sleep(1500L)
                exception = e
            }

        }

        throw exception!!
    }

    fun sendPublicMessage(chatId: Int, message: String) {
        try {
            vk.messages().send(groupActor).peerId(chatId).message(message).executeWithWaitingRetries()
        } catch (e: Exception) {
            println("Can not send message to chat $chatId: $message")
        }
    }

    fun sendMessage(user: Int, message: String) {
        try {
            vk.messages().send(groupActor).userId(user).message(message).executeWithWaitingRetries()
        } catch (e: Exception) {
            println("Can not send message to ${user}: $message")
        }
    }

    private fun getLastChatId(): Int {
        val dialogs = vk.messages().getDialogs(groupActor).count(100).executeWithWaitingRetries()
        return dialogs.items.mapNotNull { it.message.chatId }.firstOrNull()
                ?: throw IllegalStateException("Can not find any conversation")
    }

}