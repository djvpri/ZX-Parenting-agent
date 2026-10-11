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
}
