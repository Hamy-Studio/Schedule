package it.hamy.shedule

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GroupSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GroupAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_selection)

        recyclerView = findViewById(R.id.groupRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val groups = listOf(
            "ИСП-22", "ИСП-21", "ИСП-23п", "ИСВ-22", "ИСВ-21" // Добавьте все группы
        )

        adapter = GroupAdapter(groups) { selectedGroup ->
            PreferencesManager.saveGroup(this, selectedGroup)
            Toast.makeText(this, "Вы выбрали группу: $selectedGroup", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        recyclerView.adapter = adapter
    }
}