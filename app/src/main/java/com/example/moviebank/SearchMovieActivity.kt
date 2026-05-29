package com.example.moviebank

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.moviebank.data.Movie
import com.example.moviebank.data.MovieDao
import com.example.moviebank.data.MovieDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

class SearchMovieActivity: AppCompatActivity() {
    var title_edt: EditText? = null
    lateinit var movieResults: TextView
    lateinit var moviePoster: ImageView

    lateinit var searchBtn: Button
    lateinit var addToDBBtn: Button

    var url_string: String? = null
    var MY_API_KEY: String? = null
    var counter: Int = 0
    var title: String? = ""
    var year: String? = null
    var rated: String? = null
    var released: String? = null
    var runtime: String? = null
    var genre: String? = null
    var director: String? = null
    var writer: String? = null
    var actors: String? = null
    var plot: String? = null
    var posterLink: String? = ""
    var bitMapImage: Bitmap? = null
    var response: String? = null
    var isPosterFound: Boolean = false 
    var isMovieFound: Boolean = false 
    var savedMovieResults: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_movie)
        title_edt = findViewById(R.id.textInputEditText)
        movieResults = findViewById(R.id.movie_details)
        moviePoster = findViewById(R.id.movie_poster)
        searchBtn = findViewById(R.id.searchMovie_btn)
        addToDBBtn = findViewById(R.id.addToDB_btn)

        MY_API_KEY = resources.getString(R.string.MY_API_KEY)

        if (savedInstanceState != null) {
            title = savedInstanceState.getString("title","")
            posterLink = savedInstanceState.getString("posterLink","N/A")
            savedMovieResults = savedInstanceState.getString("movieResults","No results")
            isMovieFound = savedInstanceState.getBoolean("isMovieFound",false)
            isPosterFound = savedInstanceState.getBoolean("isPosterFound",false)
            
            runBlocking {
                withContext(Dispatchers.IO) {
                    try {
                        bitMapImage = getBitMapPicture()
                    } catch (e: Exception) {
                        println("Exception caught: $e")
                    }
                }
                if (savedMovieResults!!.isEmpty()) moviePoster.setImageResource(0)
                else moviePoster.setImageBitmap(bitMapImage)
            }
            movieResults.text = savedMovieResults
        }

        val db = Room.databaseBuilder(this, MovieDatabase::class.java, "Movie_Bank_Database").build()
        val movieDao = db.getDao()

        searchBtn.setOnClickListener {
            getMovie()
        }

        addToDBBtn.setOnClickListener {
            if (movieResults.text.trim().equals("")) {
                Toast.makeText(addToDBBtn.context
                    ,"Please search for a movie first before attempting to add it to the database."
                    ,Toast.LENGTH_LONG).show()
            } else {
                if (movieAlreadyExists(movieDao)) {
                    Toast.makeText(addToDBBtn.context
                        , "Cannot add this movie as it already exists in the database!"
                        ,Toast.LENGTH_LONG).show()
                } else {
                    addMovie(movieDao)
                    Toast.makeText(addToDBBtn.context
                        , "$title was added to the database!"
                        ,Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun movieAlreadyExists(dao: MovieDao): Boolean {
        var result: Boolean = false
        runBlocking {
            launch {
                val allMovies = dao.getAllMovies()
                for (movie in allMovies) {
                    if (movie.title.equals(title)) {
                        result = true
                    }
                }
            }
        }
        return result
    }

    private fun addMovie(dao: MovieDao) {
        runBlocking {
            launch {
                counter = if (dao.getAllMovies().isEmpty()) 1 else (dao.getHighestId() ?: 0) + 1
                dao.addMovie(Movie(
                    counter,
                    title,
                    year,
                    rated,
                    released,
                    runtime,
                    genre,
                    director,
                    writer,
                    actors,
                    plot))
            }
        }
    }

    private fun getMovie() {
        val movieName = title_edt!!.text.toString().trim()
        url_string = "https://www.omdbapi.com/?t=" + movieName + "&apikey=" + MY_API_KEY
        var data: String = ""

        runBlocking {
            withContext(Dispatchers.IO) {
                var movieData = StringBuilder("")
                var url = URL(url_string)
                var con = url.openConnection() as HttpURLConnection
                    try {
                        val reader = BufferedReader(InputStreamReader(con.inputStream))
                        var line = reader.readLine()
                        while (line != null) {
                            movieData.append(line)
                            line = reader.readLine()
                        }
                        data = parseJSON(movieData)
                        bitMapImage = getBitMapPicture()

                    } catch (e: Exception) {
                        println("Exception caught: " + e)
                    }
            }
            movieResults.text = data
            if (isMovieFound) {
                if (isPosterFound){
                    moviePoster.setImageBitmap(bitMapImage)
                    isMovieFound = false
                    isPosterFound = false
                } else {
                    moviePoster.setImageResource(R.drawable.not_available)
                }
            } else {
                moviePoster.setImageResource(0)
                Toast.makeText(searchBtn.context
                    , "Error: Movie that was searched cannot be retrieved as it does not exist." +
                            "\nPlease try entering the title of the movie in full."
                    ,Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getBitMapPicture(): Bitmap {
        var bitmap: Bitmap? = null
        var url = URL(posterLink)
        val con = url.openConnection() as HttpURLConnection
        val bf = BufferedInputStream(con.inputStream)
        bitmap = BitmapFactory.decodeStream(bf)
        return bitmap
    }

    private fun parseJSON(data: StringBuilder): String {
        val JAO = JSONObject(data.toString())

        title = JAO.optString("Title", "")
        year = JAO.optString("Year", "")
        rated = JAO.optString("Rated", "")
        released = JAO.optString("Released", "")
        runtime = JAO.optString("Runtime", "")
        genre = JAO.optString("Genre", "")
        director = JAO.optString("Director", "")
        writer = JAO.optString("Writer", "")
        actors = JAO.optString("Actors", "")
        plot = JAO.optString("Plot", "")
        response = JAO.optString("Response", "False")
        posterLink = JAO.optString("Poster", "")

        isPosterFound = !(posterLink.equals("N/A"))
        isMovieFound = response.equals("True")

        return "Title: " + title + "\n" + "Year: " + year + "\n" + "Rated: " + rated +
                "\n" + "Released: " + released + "\n" + "Runtime: " + runtime + "\n" + "Genre: " +
                genre + "\n" + "Director: " + director + "\n" + "Writer: " + writer + "\n" + "Actors: " +
                actors + "\n" + "Plot: " + plot
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("movieResults",movieResults.text.toString())
        outState.putString("posterLink",posterLink)
        outState.putString("title",title)
        outState.putBoolean("isMovieFound",isMovieFound)
        outState.putBoolean("isPosterFound",isPosterFound)
    }
}