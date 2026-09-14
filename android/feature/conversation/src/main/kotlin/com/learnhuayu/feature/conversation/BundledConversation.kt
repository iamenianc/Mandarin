package com.learnhuayu.feature.conversation

import com.learnhuayu.core.ai.ConversationTurnReply

/**
 * One starter coaching scenario (FR-11, `docs/03-design.md`): a fixed id sent as the WF-2
 * scenario, an English label and blurb for the picker, the coach opener voiced at session
 * start, and scripted fallback replies (WF-2 fallback) that keep the exchange moving
 * offline. All Mandarin is pinyin with tone numbers; there is no hanzi (ADR 0012, FR-17).
 */
data class ConversationScenario(
    val id: String,
    val label: String,
    val blurb: String,
    val targetDifficulty: String,
    val opener: ConversationTurnReply,
    val scriptedReplies: List<ConversationTurnReply>,
) {
    /** Cycles the scripted replies so consecutive offline turns keep the exchange moving. */
    fun scriptedReply(index: Int): ConversationTurnReply = scriptedReplies[index % scriptedReplies.size]
}

/**
 * Bundled starter scenarios plus their offline scripted lines. Used for the picker and for
 * every WF-2 failure, so coaching practice never dead-ends without a network (NFR-3).
 */
object BundledConversation {

    val scenarios: List<ConversationScenario> = listOf(
        ConversationScenario(
            id = "greeting",
            label = "Greeting a friend",
            blurb = "Say hello and introduce yourself.",
            targetDifficulty = "beginner",
            opener = ConversationTurnReply(
                replyText = "ni3 hao3! hen3 gao1 xing4 jian4 dao4 ni3.",
                nextPrompt = "Say ni3 hao3 back and tell the coach your name.",
            ),
            scriptedReplies = listOf(
                ConversationTurnReply(
                    replyText = "hen3 hao3! ni3 shuo1 de5 hen3 qing1 chu5.",
                    gentleCorrection = "Try tone 3 as a low dip: hao3.",
                    nextPrompt = "Ask the coach: ni3 hao3 ma5?",
                ),
                ConversationTurnReply(
                    replyText = "dui4! zai4 shuo1 yi1 ci4, man4 yi1 dian3.",
                    nextPrompt = "Repeat your last line more slowly.",
                ),
            ),
        ),
        ConversationScenario(
            id = "ordering",
            label = "Ordering food",
            blurb = "Order a dish and ask for the check.",
            targetDifficulty = "beginner",
            opener = ConversationTurnReply(
                replyText = "ni3 hao3! xiang3 chi1 dian3 shen2 me5?",
                nextPrompt = "Order with wo3 yao4 (I want) plus a dish.",
            ),
            scriptedReplies = listOf(
                ConversationTurnReply(
                    replyText = "hao3 de5! hai2 yao4 bie2 de5 ma5?",
                    gentleCorrection = "yao4 is tone 4, short and falling.",
                    nextPrompt = "Add one more dish or say mai3 dan1 (check please).",
                ),
                ConversationTurnReply(
                    replyText = "mei2 wen4 ti2! man4 man4 chi1.",
                    nextPrompt = "Say xie4 xie5 (thank you) to finish.",
                ),
            ),
        ),
        ConversationScenario(
            id = "directions",
            label = "Asking directions",
            blurb = "Ask where places are and how far they are.",
            targetDifficulty = "beginner",
            opener = ConversationTurnReply(
                replyText = "ni3 hao3! qu4 na3 li3?",
                nextPrompt = "Ask: qing3 wen4, che1 zhan4 zai4 na3 li3? (where is the station?)",
            ),
            scriptedReplies = listOf(
                ConversationTurnReply(
                    replyText = "wang3 qian2 zou3, ran2 hou4 you4 zhuan3.",
                    gentleCorrection = "qian2 is tone 2, rising.",
                    nextPrompt = "Ask how far: yuan3 ma5?",
                ),
                ConversationTurnReply(
                    replyText = "bu2 yuan3, zou3 lu4 wu3 fen1 zhong1.",
                    nextPrompt = "Say xie4 xie5, zai4 jian4 (goodbye) to finish.",
                ),
            ),
        ),
    )

    fun scenario(id: String): ConversationScenario? = scenarios.find { it.id == id }
}
