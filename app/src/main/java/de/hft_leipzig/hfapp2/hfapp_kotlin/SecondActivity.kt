package de.hft_leipzig.hfapp2.hfapp_kotlin

import android.arch.persistence.room.Room
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView

class SessionAdapter(private val context: Context,
                    private val dataSource: ArrayList<Session>) : BaseAdapter() {

    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get view for row item
        val rowView = inflater.inflate(R.layout.session_item, parent, false)
        val titleTextView = rowView.findViewById(R.id.sessionID) as TextView
        val sess = getItem(position) as Session

        titleTextView.text = sess.sessionID

        return rowView
    }
}

class SecondActivity : AppCompatActivity() {
    var sessionList: ArrayList<Session> = ArrayList()
    private lateinit var listView: ListView
    private lateinit var db: AppDatabase

    internal inner class GetSessionsFromDB : Runnable {
        override fun run() {
            val listSessionList = db.measurementPointDao().getAllSessions()
            sessionList = ArrayList(listSessionList)
        }
    }

    private fun getSessions() {
        val t = Thread(GetSessionsFromDB())
        t.start()
        t.join()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "measurements"
        ).build()

        getSessions()

        listView = findViewById(R.id.lvSessions)

        val listItems = arrayOfNulls<String>(sessionList.size)
        for (i in 0 until sessionList.size) {
            val recipe = sessionList[i]
            listItems[i] = recipe.sessionID
        }

        val adapter = SessionAdapter(this, sessionList)
        listView.adapter = adapter
    }

}
