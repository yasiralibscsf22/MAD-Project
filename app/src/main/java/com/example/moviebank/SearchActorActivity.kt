package com.example.moviebank

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.moviebank.data.MovieDao
import com.example.moviebank.data.MovieDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SearchActorActivity: AppCompatActivity() {
    lateinit var search_actor_et: EditText
    lateinit var search_btn: Button
    lateinit var search_actor_tv: TextView

    var MY_API_KEY: String? = null
    var savedActorResults: String? = "" 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_actor)
        search_actor_et = findViewById(R.id.search_actor_et)
        search_btn = findViewById(R.id.searchActor_btn)
        search_actor_tv = findViewById(R.id.search_actor_tv)

        MY_API_KEY = resources.getString(R.string.MY_API_KEY)

        val db = Room.databaseBuilder(this, MovieDatabase::class.java, "Movie_Bank_Database").build()
        val movieDao = db.getDao()

        if (savedInstanceState != null){
            savedActorResults = savedInstanceState.getString("savedActorResults","None")
            search_actor_tv.setText(savedActorResults)
        }

        search_btn.setOnClickListener {
            if (searchForActor(movieDao) && !search_actor_et.text.equals("")) Toast.makeText(search_btn.context,"Actors found",Toast.LENGTH_LONG).show()
            else Toast.makeText(search_btn.context,"Couldn't find any actors",Toast.LENGTH_LONG).show()
        }
    }

    private fun searchForActor(dao: MovieDao): Boolean {
        search_actor_tv.text = ""
        var foundActor = false
        var userInput = search_actor_et.text.toString().lowercase().trim()
        runBlocking {
            launch {
                val allMovies = dao.getAllMovies()
                for (movie in allMovies) {
                    if (movie.actors?.lowercase()?.contains(userInput) == true) {
                        search_actor_tv.append("Title: " + movie.title + "\nYear: " + movie.year +
                        "\nActors: " + movie.actors)
                        search_actor_tv.append("\n\n")
                        foundActor = true
                    }
                }
            }
        }
        return foundActor
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("savedActorResults",search_actor_tv.text.toString())
    }
}