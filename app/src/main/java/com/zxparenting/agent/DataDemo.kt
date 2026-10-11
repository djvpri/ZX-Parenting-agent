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
}
