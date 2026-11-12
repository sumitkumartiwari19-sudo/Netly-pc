package com.skt.netly.pc.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf

val LocalLanguage = staticCompositionLocalOf { "en" }

/**
 * English text is the key. In Hindi mode every NeuText looks its text up here; text that is not listed stays in English.
 * Texts that join parts with " · " or end in a count like " (3)" are translated part by part.
 */
object Strings {
    private val hindi: Map<String, String> = mapOf(
        // Navigation and titles
        "Home" to "होम",
        "Search" to "खोजें",
        "Downloads" to "डाउनलोड",
        "Library" to "लाइब्रेरी",
        "Settings" to "सेटिंग्स",
        "Light" to "लाइट",
        "Dark" to "डार्क",
        "System" to "सिस्टम",
        // Home
        "Download anything from YouTube" to "YouTube से कुछ भी डाउनलोड करें",
        "Paste a link, pick a quality, and Netly does the rest." to "लिंक पेस्ट करें, क्वालिटी चुनें, बाकी Netly करेगा।",
        "Paste a YouTube link…" to "YouTube लिंक पेस्ट करें…",
        "Paste a YouTube link above to get started" to "शुरू करने के लिए ऊपर YouTube लिंक पेस्ट करें",
        "Paste" to "पेस्ट",
        "Fetch" to "खोजें",
        "Fetching…" to "लोड हो रहा है…",
        "Fetching video details…" to "वीडियो की जानकारी ली जा रही है…",
        "This usually takes a few seconds." to "इसमें आमतौर पर कुछ सेकंड लगते हैं।",
        "Could not fetch this video" to "यह वीडियो नहीं मिल सका",
        "Something went wrong." to "कुछ गड़बड़ हो गई।",
        "Retry" to "फिर कोशिश करें",
        "Video" to "वीडियो",
        "Audio" to "ऑडियो",
        "Shorts" to "शॉर्ट्स",
        "Audio only" to "सिर्फ ऑडियो",
        "VIDEO QUALITY" to "वीडियो क्वालिटी",
        "AUDIO FORMAT" to "ऑडियो फॉर्मेट",
        "Format" to "फॉर्मेट",
        "Embed subtitles" to "सबटाइटल जोड़ें",
        "Embed thumbnail & tags" to "थंबनेल और टैग जोड़ें",
        "Saving to" to "यहाँ सेव होगा",
        "Add to queue" to "कतार में जोड़ें",
        "Download" to "डाउनलोड",
        "Pick a quality first." to "पहले क्वालिटी चुनें।",
        "Added to the queue. See the Downloads tab." to "कतार में जुड़ गया। डाउनलोड टैब देखें।",
        "Download started. Follow it in the Downloads tab." to "डाउनलोड शुरू हो गया। डाउनलोड टैब में देखें।",
        "Playlists are coming soon" to "प्लेलिस्ट जल्द आ रही हैं",
        "Preparing engine…" to "इंजन तैयार हो रहा है…",
        "Engine problem" to "इंजन में समस्या",
        "YouTube link" to "YouTube लिंक",
        "YouTube link copied" to "YouTube लिंक कॉपी हुआ",
        "Dismiss" to "बंद करें",
        // Downloads
        "Everything Netly is fetching right now." to "Netly अभी जो भी डाउनलोड कर रहा है।",
        "Pause all" to "सब रोकें",
        "Resume all" to "सब चालू करें",
        "Clear completed" to "पूरे हुए हटाएं",
        "Total speed" to "कुल स्पीड",
        "Active" to "चालू",
        "Queued" to "कतार में",
        "Completed" to "पूरे हुए",
        "Failed" to "असफल",
        "All" to "सभी",
        "Pause" to "रोकें",
        "Resume" to "चालू करें",
        "Cancel" to "रद्द करें",
        "Remove" to "हटाएं",
        "Show in folder" to "फोल्डर में दिखाएं",
        "Nothing downloading" to "कुछ डाउनलोड नहीं हो रहा",
        "Nothing in this tab" to "इस टैब में कुछ नहीं है",
        "Try another tab." to "दूसरा टैब देखें।",
        "Go to Home" to "होम पर जाएं",
        "Paste a YouTube link on Home to start your first download." to "पहला डाउनलोड शुरू करने के लिए होम पर YouTube लिंक पेस्ट करें।",
        "Waiting in the queue" to "कतार में इंतज़ार",
        "Merging and finishing…" to "जोड़ा जा रहा है…",
        "Paused" to "रुका हुआ",
        "The download failed." to "डाउनलोड नहीं हो सका।",
        // Library
        "Your finished downloads, ready to play offline." to "आपके पूरे हुए डाउनलोड, ऑफलाइन चलाने के लिए तैयार।",
        "Search your downloads…" to "अपने डाउनलोड खोजें…",
        "Newest" to "नया पहले",
        "Name" to "नाम",
        "No matches" to "कुछ नहीं मिला",
        "Try another search or filter." to "दूसरी खोज या फ़िल्टर आज़माएं।",
        "Your downloads will show up here" to "आपके डाउनलोड यहाँ दिखेंगे",
        "Finished videos and music appear in this library." to "पूरे हुए वीडियो और गाने इस लाइब्रेरी में दिखेंगे।",
        "File not found. It may have been moved or deleted." to "फ़ाइल नहीं मिली। शायद हटा दी गई या कहीं और चली गई।",
        "Play" to "चलाएं",
        "Folder" to "फोल्डर",
        "Delete" to "हटाएं",
        "Sure?" to "पक्का?",
        // Search and playlists
        "Search videos and playlists on YouTube." to "YouTube पर वीडियो और प्लेलिस्ट खोजें।",
        "Search YouTube…" to "YouTube पर खोजें…",
        "Videos" to "वीडियो",
        "Playlists" to "प्लेलिस्ट",
        "Playlist" to "प्लेलिस्ट",
        "Open playlist" to "प्लेलिस्ट खोलें",
        "Options" to "विकल्प",
        "Searching…" to "खोज रहे हैं…",
        "No results found." to "कोई नतीजा नहीं मिला।",
        "Type something to search, or paste a YouTube link." to "खोजने के लिए कुछ लिखें, या YouTube लिंक पेस्ट करें।",
        "Back" to "वापस",
        "Select all" to "सब चुनें",
        "Select none" to "कुछ नहीं",
        "Loading playlist…" to "प्लेलिस्ट लोड हो रही है…",
        "Nothing selected" to "कुछ चुना नहीं गया",
        "Quality" to "क्वालिटी",
        "Best" to "सबसे अच्छी",
        // Settings
        "Make Netly work the way you like." to "Netly को अपने हिसाब से चलाएं।",
        "General" to "सामान्य",
        "Language" to "भाषा",
        "Theme" to "थीम",
        "Smooth mode (fewer effects, for older PCs)" to "स्मूद मोड (कम इफ़ेक्ट, पुराने PC के लिए)",
        "Watch the clipboard for YouTube links" to "क्लिपबोर्ड में YouTube लिंक देखें",
        "Show notifications" to "नोटिफिकेशन दिखाएं",
        "Keep running in the tray when closed" to "बंद करने पर ट्रे में चालू रखें",
        "Start with Windows" to "Windows के साथ शुरू करें",
        "YouTube access" to "YouTube एक्सेस",
        "Use cookies from browser" to "ब्राउज़र की कुकीज़ इस्तेमाल करें",
        "Pick the browser where you are signed in to YouTube if a video asks you to sign in." to "अगर वीडियो साइन-इन मांगे तो वह ब्राउज़र चुनें जिसमें आप YouTube पर साइन-इन हैं।",
        "Proxy" to "प्रॉक्सी",
        "Test connection" to "कनेक्शन जाँचें",
        "Testing…" to "जाँच हो रही है…",
        "Change" to "बदलें",
        "Default quality" to "डिफ़ॉल्ट क्वालिटी",
        "At the same time" to "एक साथ",
        "Speed limit" to "स्पीड सीमा",
        "Unlimited" to "असीमित",
        "File name" to "फ़ाइल का नाम",
        "Title" to "टाइटल",
        "Channel – Title" to "चैनल – टाइटल",
        "Engine and about" to "इंजन और जानकारी",
        "Check for updates" to "अपडेट जाँचें",
        "Updating…" to "अपडेट हो रहा है…",
        "Update the engine automatically" to "इंजन अपने आप अपडेट करें",
        "Starting…" to "शुरू हो रहा है…",
        "None" to "कोई नहीं",
        // Player
        "Speed" to "स्पीड",
        "Loading…" to "लोड हो रहा है…",
    )

    private val countSuffix = Regex("""^(.*?)( \(\d+\))$""")

    fun tr(language: String, text: String): String {
        if (language != "hi" || text.isEmpty()) return text
        hindi[text]?.let { return it }
        countSuffix.matchEntire(text)?.let { m ->
            val base = hindi[m.groupValues[1]]
            if (base != null) return base + m.groupValues[2]
        }
        if (" · " in text) {
            val parts = text.split(" · ")
            val mapped = parts.map { part -> hindi[part] ?: translatePercent(part) }
            if (mapped.indices.any { mapped[it] != parts[it] }) return mapped.joinToString(" · ")
        }
        return text
    }

    /** "42% left"-style pieces and "Paused" with a percent. */
    private fun translatePercent(part: String): String = when {
        part.endsWith(" left") -> part.removeSuffix(" left") + " बाकी"
        else -> part
    }

    fun hasTranslation(text: String): Boolean = hindi.containsKey(text)
}
