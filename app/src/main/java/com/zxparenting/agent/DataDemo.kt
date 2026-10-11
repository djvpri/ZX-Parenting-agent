package com.zxparenting.agent

// Sample data untuk Play Console reviewer demo mode.
// Dipakai saat Simpanan.isDemo() == true — tidak ada API call.

object DataDemo {
    val officialItems = listOf(
        OfficialItem(
            id = "demo1",
            tipe = "KUIS_HARIAN",
            judul = "Fun Math: Addition",
            deskripsi = "Practice adding numbers 1-20 with picture questions!",
            konten = null,
            meta = OfficialMeta(
                kategori = "Math",
                rentangUmur = "6-10",
                levelKesulitan = 2,
                tokenReward = 15,
                premium = false,
                views = 1247,
                aiGenerated = false,
            ),
        ),
        OfficialItem(
            id = "demo2",
            tipe = "TANTANGAN_MINGGUAN",
            judul = "Weekly Challenge: Natural Science",
            deskripsi = "Explore the world of animals and plants!",
            konten = null,
            meta = OfficialMeta(
                kategori = "Science",
                rentangUmur = "7-12",
                levelKesulitan = 3,
                tokenReward = 50,
                premium = false,
                views = 856,
                aiGenerated = false,
            ),
        ),
        OfficialItem(
            id = "demo3",
            tipe = "ARTIKEL",
            judul = "Why Should We Sleep on Time?",
            deskripsi = "A fun story about why sleep is important.",
            konten = null,
            meta = OfficialMeta(
                kategori = "Health",
                rentangUmur = "6-10",
                levelKesulitan = 1,
                tokenReward = 10,
                premium = false,
                views = 2341,
                aiGenerated = false,
            ),
        ),
        OfficialItem(
            id = "demo4",
            tipe = "VIDEO_PENDEK",
            judul = "How to Make an Origami Bird",
            deskripsi = "Follow along to fold a paper bird!",
            konten = null,
            meta = OfficialMeta(
                kategori = "Craft",
                rentangUmur = "7-12",
                levelKesulitan = 1,
                tokenReward = 10,
                premium = false,
                views = 1567,
                aiGenerated = false,
            ),
        ),
    )

    val tugas = listOf(
        TugasAnak(
            id = "demo-t1",
            judul = "English Quiz: Animals",
            deskripsi = "Match animal names with pictures.",
            reward = 25,
            status = "pending",
            deadline = "2026-10-12T21:00:00Z",
            type = "ai_quiz",
        ),
        TugasAnak(
            id = "demo-t2",
            judul = "Read a storybook for 30 minutes",
            deskripsi = "Read any book you like!",
            reward = 20,
            status = "done",
            deadline = null,
            type = "manual",
        ),
    )

    // --- Hadiah tab demo data ---

    val saldo = 320

    val katalog = listOf(
        HadiahItem("demo-h1", "Lego Set", "Build your own city!", 50, 3, false),
        HadiahItem("demo-h2", "Ice Cream", "Pick your favorite flavor", 20, 5, true),
        HadiahItem("demo-h3", "Game Voucher", "30 min game time", 100, 0, false),
    )

    val quests = listOf(
        QuestAnak("demo-q1", "Help Mom clean up", "Tidy your room", 30, "2026-10-12T21:00:00Z",
            listOf(QuestAnggota("demo", false))),
        QuestAnak("demo-q2", "Read a book 30 min", "Any book you like", 20, null,
            listOf(QuestAnggota("demo", true))),
    )

    val challenge = ChallengeRes("demo-c1", "Perfect Quiz", "Math multiplication", 5, false)

    val streak = StreakRes(current = 7, terbaik = 12, terakhir = "2026-10-11")

    val ledger = listOf(
        LedgerItem("demo-l1", "KUIS", 15, "Math Addition", null, "2026-10-11T10:30:00Z"),
        LedgerItem("demo-l2", "PEMAKAIAN", -30, "Tukar 30 min play", null, "2026-10-11T09:15:00Z"),
        LedgerItem("demo-l3", "TUGAS", 25, "English: Animals", null, "2026-10-10T14:00:00Z"),
        LedgerItem("demo-l4", "CHALLENGE", 5, "Perfect Quiz", null, "2026-10-10T11:00:00Z"),
        LedgerItem("demo-l5", "TANTANGAN", 50, "Natural Science", null, "2026-10-09T15:00:00Z"),
    )

    val pinjam = PinjamRes(hutang = 0, riwayat = emptyList())

    // --- Demo kuis detail (utk Play Console reviewer) ---

    val demoSoal = mapOf(
        "demo-t1" to listOf(
            Soal("Which animal says 'moo'?", listOf("Dog", "Cat", "Cow", "Duck")),
            Soal("Which animal says 'meow'?", listOf("Dog", "Cat", "Cow", "Bird")),
            Soal("Which animal is known as man's best friend?", listOf("Cat", "Dog", "Fish", "Snake")),
            Soal("Which animal hops?", listOf("Elephant", "Rabbit", "Turtle", "Whale")),
            Soal("Which animal swims?", listOf("Eagle", "Fish", "Lion", "Monkey")),
        ),
        "demo1" to listOf(
            Soal("What is 5 + 3?", listOf("6", "7", "8", "9")),
            Soal("What is 10 + 5?", listOf("13", "14", "15", "16")),
            Soal("What is 2 + 7?", listOf("8", "9", "10", "11")),
            Soal("What is 4 + 4?", listOf("7", "8", "9", "10")),
            Soal("What is 6 + 3?", listOf("8", "9", "10", "11")),
        ),
        "demo2" to listOf(
            Soal("What do plants need to grow?", listOf("Sunlight", "Darkness", "Cold", "Noise")),
            Soal("Which animal lays eggs?", listOf("Dog", "Cat", "Chicken", "Cow")),
            Soal("What is the largest mammal?", listOf("Elephant", "Blue Whale", "Giraffe", "Horse")),
            Soal("Where do fish live?", listOf("Sky", "Forest", "Water", "Desert")),
            Soal("What color are most leaves?", listOf("Red", "Blue", "Green", "Purple")),
        ),
        "demo-c1" to listOf(
            Soal("What is 3 × 4?", listOf("7", "12", "14", "16")),
            Soal("What is 6 × 2?", listOf("8", "10", "12", "14")),
            Soal("What is 5 × 3?", listOf("10", "12", "15", "18")),
            Soal("What is 7 × 2?", listOf("12", "14", "16", "18")),
            Soal("What is 4 × 4?", listOf("12", "14", "16", "18")),
        ),
    )

    val demoJawaban = mapOf(
        "demo-t1" to listOf(2, 1, 1, 1, 1),
        "demo1" to listOf(2, 2, 1, 1, 1),
        "demo2" to listOf(0, 2, 1, 2, 2),
        "demo-c1" to listOf(1, 2, 2, 1, 2),
    )

    val demoDetail = mapOf(
        "demo3" to OfficialDetail("demo3", "ARTIKEL", "Why Should We Sleep on Time?",
            "Sleep is important for growing bodies. When you sleep, your brain rests and your body grows stronger. Kids who sleep 9-11 hours feel happier and do better in school. Going to bed on time means you have energy to play and learn the next day!",
            null, "Health", 10, false, false),
        "demo4" to OfficialDetail("demo4", "VIDEO_PENDEK", "How to Make an Origami Bird",
            "Follow along to fold a paper bird! All you need is a square piece of paper. Start by folding it diagonally, then fold the corners to make wings. Watch the full video in the app!",
            null, "Craft", 10, false, false),
    )
}
