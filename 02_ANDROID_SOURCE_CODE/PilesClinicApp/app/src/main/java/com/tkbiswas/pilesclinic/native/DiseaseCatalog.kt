package com.tkbiswas.pilesclinic.native

/**
 * Disease educational content — ported VERBATIM from the web app.js diseaseData()
 * so the Bangla / Hindi / English text and spelling match the website exactly.
 * (Do not hand-edit the strings; they are copied from the source of truth.)
 */
data class DiseaseInfo(
    val key: String, val nameEn: String, val nameBn: String, val nameHi: String,
    val descEn: String, val descBn: String, val descHi: String,
    val symptoms: List<String>, val cause: String, val treat: String
)

object DiseaseCatalog {
    val list: List<DiseaseInfo> = listOf(
        DiseaseInfo(
            key = "Piles", nameEn = "Piles", nameBn = "পাইলস (অর্শ)", nameHi = "पाइल्स (बवासीर)",
            descEn = "Piles (haemorrhoids) are swollen, inflamed veins in the lower rectum and anus. They form when the cushions of tissue that normally help control bowel movements become enlarged from repeated straining or pressure. Piles can be internal (inside the rectum, usually painless but may bleed) or external (under the skin around the anus, often painful and itchy). Left untreated, they tend to worsen gradually, so early Ayurvedic care matters.",
            descBn = "অর্শ (পাইলস) হলো মলদ্বার ও মলদ্বারের নিচের অংশের শিরাগুলো ফুলে যাওয়া একটি সমস্যা। বারবার চাপ পড়া বা কোষ্ঠকাঠিন্যের কারণে এই শিরাগুলো ধীরে ধীরে ফুলে ওঠে। অর্শ দুই রকম হতে পারে — অভ্যন্তরীণ (ভেতরের দিকে, সাধারণত ব্যথাহীন কিন্তু রক্ত পড়তে পারে) এবং বাহ্যিক (মলদ্বারের চারপাশে চামড়ার নিচে, প্রায়ই ব্যথা ও চুলকানি সহ)। অবহেলা করলে ধীরে ধীরে সমস্যা বাড়তে থাকে, তাই প্রথম দিকেই আয়ুর্বেদিক চিকিৎসা নেওয়া জরুরি।",
            descHi = "बवासीर (पाइल्स) गुदा और मलाशय के निचले हिस्से की नसों में सूजन की समस्या है। बार-बार दबाव पड़ने या कब्ज की वजह से ये नसें धीरे-धीरे फूल जाती हैं। बवासीर दो तरह की होती है — आंतरिक (अंदर की ओर, आमतौर पर दर्द रहित पर खून आ सकता है) और बाहरी (गुदा के आसपास त्वचा के नीचे, अक्सर दर्द और खुजली के साथ)। अनदेखा करने पर समस्या धीरे-धीरे बढ़ती जाती है, इसलिए शुरुआत में ही आयुर्वेदिक इलाज ज़रूरी है.",
            symptoms = listOf("Bleeding during/after stool", "Pain or discomfort while sitting", "Itching around the anus", "Swelling or a lump near the anus", "Feeling of incomplete evacuation"),
            cause = "Chronic constipation, long straining during stool, prolonged sitting (desk jobs, long travel), low-fibre diet, pregnancy, obesity, and heavy lifting all increase pressure on the rectal veins and raise the risk of piles.",
            treat = "Ayurvedic consultation with diet correction (high-fibre, adequate water), specific herbal medicines (Kshar/Taila as advised), sitz-bath guidance where needed, and regular follow-up to track improvement — surgery is avoided unless truly necessary."
        ),
        DiseaseInfo(
            key = "Fissure", nameEn = "Fissure", nameBn = "ফিশার", nameHi = "फिशर",
            descEn = "An anal fissure is a small tear or crack in the thin, moist lining of the anus. It most commonly results from passing hard or large stools, which stretches the anal canal beyond its capacity. The tear exposes sensitive tissue and underlying muscle, causing a sharp, cutting pain during and after bowel movements that can last minutes to hours. Chronic fissures may cause the surrounding muscle to spasm, making healing slower without proper care.",
            descBn = "ফিশার হলো মলদ্বারের পাতলা, নরম আবরণে হওয়া একটি ছোট ছিঁড়ে যাওয়া বা ফাটল। এটি সাধারণত শক্ত বা বড় মল ত্যাগের কারণে হয়, যা মলদ্বারের নালিকে তার স্বাভাবিক ক্ষমতার চেয়ে বেশি টেনে ধরে। এই ফাটলের কারণে ভেতরের সংবেদনশীল টিস্যু ও মাংসপেশি উন্মুক্ত হয়ে যায়, ফলে মলত্যাগের সময় ও পরে তীব্র কাটা কাটা ব্যথা হয় যা কয়েক মিনিট থেকে কয়েক ঘণ্টা পর্যন্ত থাকতে পারে। দীর্ঘদিনের ফিশারে চারপাশের পেশিতে খিঁচুনি হতে পারে, যার ফলে সঠিক যত্ন ছাড়া সারতে দেরি হয়।",
            descHi = "फिशर गुदा की पतली, नरम परत में होने वाली एक छोटी दरार या चीरा है। यह आमतौर पर सख्त या बड़ा मल त्यागने से होता है, जो गुदा नली को उसकी सामान्य क्षमता से ज़्यादा खींच देता है। इस दरार से भीतर की संवेदनशील परत और मांसपेशी खुल जाती है, जिससे मल त्याग के समय और बाद में तेज़, काटने जैसा दर्द होता है जो कुछ मिनट से लेकर कई घंटों तक रह सकता है। पुरानी फिशर में आसपास की मांसपेशी में ऐंठन हो सकती है, जिससे सही देखभाल के बिना ठीक होने में देर लगती है.",
            symptoms = listOf("Sharp cutting pain during stool", "Burning sensation after stool", "Bright red bleeding on stool/tissue", "Visible small tear or skin tag", "Muscle spasm around the anus"),
            cause = "Hard or large stools, chronic constipation or diarrhoea, straining, childbirth, and reduced blood flow to the anal area are the most common causes.",
            treat = "Diet correction to soften stool, warm sitz baths, Ayurvedic healing ointments/medicines, and lifestyle guidance to reduce straining — with doctor follow-up until the tear fully heals."
        ),
        DiseaseInfo(
            key = "Fistula", nameEn = "Fistula", nameBn = "ভগন্দর (ফিস্টুলা)", nameHi = "भगंदर (फिस्टुला)",
            descEn = "A fistula-in-ano is an abnormal tunnel that forms between the inside of the anal canal and the skin near the anus. It usually develops after an anal abscess (a pocket of infection) does not heal completely, leaving behind a channel through which pus or fluid continues to drain. Because the tunnel can branch and become complex over time, fistulas often need more structured treatment than piles or fissures, including the traditional Ayurvedic Ksharsutra (medicated thread) approach in appropriate cases.",
            descBn = "ভগন্দর (ফিস্টুলা) হলো মলদ্বারের ভেতরের অংশ ও তার পাশের চামড়ার মধ্যে তৈরি হওয়া একটি অস্বাভাবিক নালি। এটি সাধারণত মলদ্বারের কোনো ফোঁড়া (সংক্রমণের পকেট) সম্পূর্ণ না সারলে তৈরি হয়, যার ফলে একটি নালি থেকে যায় যার মধ্য দিয়ে পুঁজ বা তরল ক্রমাগত বের হতে থাকে। সময়ের সাথে সাথে এই নালি শাখা-প্রশাখায় জটিল হয়ে উঠতে পারে বলে, অর্শ বা ফিশারের চেয়ে ভগন্দরের চিকিৎসায় বেশি পরিকল্পিত পদ্ধতি লাগে — উপযুক্ত ক্ষেত্রে ঐতিহ্যবাহী আয়ুর্বেদিক ক্ষারসূত্র (ওষুধযুক্ত সুতো) পদ্ধতি ব্যবহার করা হয়।",
            descHi = "भगंदर (फिस्टुला) गुदा नली के भीतरी हिस्से और उसके पास की त्वचा के बीच बनने वाला एक असामान्य रास्ता है। यह आमतौर पर गुदा के किसी फोड़े (संक्रमण की थैली) के पूरी तरह न भरने से बनता है, जिससे एक रास्ता रह जाता है जिससे पस या तरल लगातार निकलता रहता है। समय के साथ यह रास्ता शाखाओं में बंटकर जटिल हो सकता है, इसलिए बवासीर या फिशर से ज़्यादा व्यवस्थित इलाज की ज़रूरत होती है — उपयुक्त मामलों में पारंपरिक आयुर्वेदिक क्षारसूत्र (औषधीय धागा) विधि अपनाई जाती है.",
            symptoms = listOf("Persistent pus or fluid discharge", "Recurrent pain and swelling", "Repeated abscess/infection at the same spot", "Skin irritation around the opening", "Occasional fever during flare-ups"),
            cause = "An unhealed or recurrent anal abscess is the leading cause; conditions affecting bowel healing, chronic infection, and delayed treatment increase the chance of fistula formation.",
            treat = "Detailed clinical assessment to map the tract, Ayurvedic Ksharsutra therapy where suitable, supportive medicines to control infection and promote healing, and scheduled follow-up visits to monitor closure."
        ),
        DiseaseInfo(
            key = "Hydrocele", nameEn = "Hydrocele", nameBn = "হাইড্রোসিল (একশিরা)", nameHi = "हाइड्रोसील",
            descEn = "A hydrocele is a build-up of clear fluid in the thin sac that surrounds a testicle, leading to swelling of the scrotum. It is usually painless in adults, though it can cause a feeling of heaviness or dragging discomfort, especially by the end of the day or after standing for long periods. Hydroceles can develop without any obvious cause, or follow minor injury, infection, or inflammation in the area — a proper examination helps rule out other causes of scrotal swelling.",
            descBn = "হাইড্রোসিল হলো অণ্ডকোষের চারপাশের পাতলা থলিতে স্বচ্ছ তরল জমে ফোলা তৈরি হওয়ার একটি সমস্যা। প্রাপ্তবয়স্কদের ক্ষেত্রে এটি সাধারণত ব্যথাহীন হয়, তবে দিনের শেষে বা দীর্ঘক্ষণ দাঁড়িয়ে থাকার পরে ভারী লাগা বা টানটান অস্বস্তি অনুভূত হতে পারে। হাইড্রোসিল কোনো স্পষ্ট কারণ ছাড়াই হতে পারে, অথবা সামান্য আঘাত, সংক্রমণ বা প্রদাহের পরে দেখা দিতে পারে — সঠিক পরীক্ষা করলে অণ্ডকোষ ফোলার অন্য কারণগুলো বাদ দেওয়া যায়।",
            descHi = "हाइड्रोसील अंडकोष के चारों ओर की पतली थैली में साफ तरल जमा होने से होने वाली सूजन है। वयस्कों में यह आमतौर पर दर्द रहित होता है, लेकिन दिन के अंत में या लंबे समय तक खड़े रहने के बाद भारीपन या खिंचाव जैसी असुविधा महसूस हो सकती है। हाइड्रोसील बिना किसी स्पष्ट कारण के हो सकता है, या मामूली चोट, संक्रमण या सूजन के बाद हो सकता है — सही जांच से अंडकोष की सूजन के अन्य कारणों को दूर किया जा सकता है.",
            symptoms = listOf("Painless swelling of the scrotum", "Heaviness or dragging sensation", "Swelling that may increase through the day", "Discomfort while walking or standing long", "Visible size difference between sides"),
            cause = "Fluid imbalance in the scrotal sac, minor injury, infection or inflammation, and in some cases no identifiable trigger at all — it can also occur congenitally in infants.",
            treat = "Clinical examination and relevant investigation to confirm the diagnosis, followed by an individualised treatment plan by the doctor — conservative management or procedure, depending on size and symptoms."
        ),
    )
    fun byKey(k: String): DiseaseInfo = list.find { it.key == k } ?: list.first()
}