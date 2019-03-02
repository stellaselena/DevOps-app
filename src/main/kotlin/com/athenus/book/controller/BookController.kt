package com.athenus.book.controller

import com.athenus.book.domain.converter.BookConverter
import com.athenus.book.domain.dto.BookDto
import com.athenus.book.repository.BookRepository
import com.codahale.metrics.Gauge
import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

import io.swagger.annotations.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.ConstraintViolationException

@Api(value = "/books", description = "API for book entities")
@RequestMapping(
    path = arrayOf("/books"),
    produces = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE)
)
@RestController
@Validated
class BookController {
    @Autowired
    private lateinit var repo: BookRepository

    @Autowired
    private lateinit var metrics: MetricRegistry

    @ApiOperation("Create new book")
    @PostMapping(consumes = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE))
    @ApiResponses(
        ApiResponse(code = 201, message = "Id of created book"),
        ApiResponse(code = 400, message = "Something is wrong with the book-body")
    )
    fun createBook(
        @ApiParam("Book to save")
        @RequestBody
        bookDto: BookDto
    ): ResponseEntity<Long> {

        metrics.meter("books").mark()


        if (!bookDto.id.isNullOrEmpty()) {
            return ResponseEntity.status(400).build()
        }

        if (!isDtoFieldsNotNull(bookDto)) {
            return ResponseEntity.status(400).build()
        }

        bookDto.name = bookDto.name!!
        bookDto.description = bookDto.description!!
        bookDto.author = bookDto.author!!
        bookDto.genre = bookDto.genre
        bookDto.price = bookDto.price
        bookDto.rating = bookDto.rating

        try {
            val savedId = repo.createBook(
                name = bookDto.name!!,
                description = bookDto.description!!,
                genre = bookDto.genre!!,
                author = bookDto.author,
                price = bookDto.price!!,
                rating = bookDto.rating!!
            )

            metrics.counter("books available").inc()


            return ResponseEntity.status(201).body(savedId)

        } catch (e: java.lang.Exception) {
            return ResponseEntity.status(400).build()
        }
    }

    @ApiOperation("Fetch all books. Can be filtered by name")
    @GetMapping
    fun getAllBooks(
        @ApiParam("Name of the book")
        @RequestParam(name = "name", required = false)
        name: String?
    ): ResponseEntity<Iterable<BookDto>> {

        metrics.meter("books").mark()

        val getBooksTimer = metrics.timer("get all books").time()


        if (name != null) {
            getBooksTimer.stop()
            return ResponseEntity.ok(BookConverter.transform(repo.findAllByName(name)))

        }
        getBooksTimer.stop()
        return ResponseEntity.ok(BookConverter.transform(repo.findAll()))

    }

    @ApiOperation("Get book specified by id")
    @GetMapping(path = arrayOf("/{id}"))
    @ApiResponses(
        ApiResponse(code = 400, message = "Id is not correct"),
        ApiResponse(code = 404, message = "Could not find book")
    )
    fun getBookById(
        @ApiParam("Id of book")
        @PathVariable("id")
        pathId: String?
    )
            : ResponseEntity<BookDto> {

        metrics.meter("books").mark()

        val id: Long
        try {
            id = pathId!!.toLong()
        } catch (e: Exception) {
            return ResponseEntity.status(400).build()
        }

        val dto = repo.findOne(id) ?: return ResponseEntity.status(404).build()

        return ResponseEntity.ok(BookConverter.transform(dto))
    }

    @ApiOperation("Get book specified by name")
    @GetMapping(path = arrayOf("/name/{name}"))
    @ApiResponses(
        ApiResponse(code = 400, message = "Name is not correct"),
        ApiResponse(code = 404, message = "Could not find book")
    )
    fun getBookByName(
        @ApiParam("Name of book")
        @PathVariable("name")
        name: String
    )
            : ResponseEntity<BookDto> {

        metrics.meter("books").mark()


        val dto = repo.findAllByName(name).firstOrNull() ?: return ResponseEntity.status(404).build()

        return ResponseEntity.ok(BookConverter.transform(dto))
    }

    @ApiOperation("Replace the data of a book")
    @PutMapping(path = arrayOf("/{id}"), consumes = arrayOf(MediaType.APPLICATION_JSON_UTF8_VALUE))
    @ApiResponses(
        ApiResponse(code = 400, message = "Something is wrong book-body sent in this request"),
        ApiResponse(code = 404, message = "Could not find book by this id"),
        ApiResponse(code = 409, message = "Cannot change the id of book in the body!")
    )
    fun updateBook(
        @ApiParam("Id defining the book.")
        @PathVariable("id")
        pathId: String?,

        @ApiParam("Data to replace old book. Id cannot be changed, and must be the same in path and RequestBody")
        @RequestBody
        bookDto: BookDto
    ): ResponseEntity<Long> {

        metrics.meter("books").mark()

        val id: Long
        try {
            id = pathId!!.toLong()
        } catch (e: Exception) {
            return ResponseEntity.status(404).build()
        }

        // Don't change ID
        if (bookDto.id != pathId) {
            return ResponseEntity.status(409).build()
        }
        if (!repo.exists(id)) {
            return ResponseEntity.status(404).build()
        }
        if (!isDtoFieldsNotNull(bookDto)) {
            return ResponseEntity.status(400).build()
        }

        try {
            val successful = repo.updateBook(
                bookDto.name!!,
                bookDto.description!!,
                bookDto.genre!!,
                bookDto.author!!,
                bookDto.price!!,
                bookDto.rating!!,
                bookDto.id!!.toLong()
            )
            if (!successful) {
                return ResponseEntity.status(400).build()
            }
            return ResponseEntity.status(204).build()
        } catch (e: ConstraintViolationException) {
            return ResponseEntity.status(400).build()
        }
    }

    @ApiOperation("Replace the price of a book")
    @PatchMapping(path = arrayOf("/{id}/price"), consumes = arrayOf(MediaType.TEXT_PLAIN_VALUE))
    @ApiResponses(
        ApiResponse(code = 204, message = "Price successfully update. No content to return"),
        ApiResponse(code = 400, message = "Something is wrong with new price value"),
        ApiResponse(code = 404, message = "Could not find book to update price for.")
    )
    fun updatePrice(
        @ApiParam("Id defining the book.")
        @PathVariable("id")
        id: Long,
        @ApiParam("New price for book. Price cannot be negative.")
        @RequestBody
        price: String
    ): ResponseEntity<Void> {

        metrics.meter("books").mark()

        var priceToInt = 0
        try {
            priceToInt = price.toInt()
        } catch (e: Exception) {

        }

        if (!repo.exists(id)) {
            return ResponseEntity.status(404).build()
        }

        if (priceToInt < 0 || priceToInt > 1000) {
            return ResponseEntity.status(400).build()
        }
        if (!repo.updatePrice(priceToInt, id)) {
            return ResponseEntity.status(400).build()
        }
        metrics.histogram("book price change").update(priceToInt)

        return ResponseEntity.status(204).build()
    }


    @ApiOperation("Modify the book using JSON Merge Patch")
    @PatchMapping(
        path = arrayOf("/{id}"),
        consumes = arrayOf("application/merge-patch+json")
    )
    fun mergePatchBook(
        @ApiParam("Id of the book")
        @PathVariable("id")
        id: Long?,
        @ApiParam("The partial patch")
        @RequestBody
        jsonPatch: String
    )
            : ResponseEntity<Void> {

        metrics.meter("books").mark()

        val mergeBookTimer = metrics.timer("merge book").time()


        val dto = repo.findOne(id) ?: return ResponseEntity.status(404).build()

        val jackson = ObjectMapper()

        val jsonNode: JsonNode
        try {
            jsonNode = jackson.readValue(jsonPatch, JsonNode::class.java)
        } catch (e: Exception) {
            return ResponseEntity.status(400).build()
        }

        if (jsonNode.has("id")) {
            return ResponseEntity.status(409).build()
        }

        metrics.histogram("request body length").update(jsonNode.size())

        var newName = dto.name
        var newDescription = dto.description
        var newGenre = dto.genre
        var newAuthor = dto.author
        var newPrice = dto.price
        var newRating = dto.rating

        //if a field is mandatory, we will keep the old value
        if (jsonNode.has("name")) {
            val nameNode = jsonNode.get("name")
            if (nameNode.isNull) {
                newName = dto.name
            } else if (nameNode.isTextual) {
                newName = nameNode.asText()
            } else {
                return ResponseEntity.status(400).build()
            }
        }

        if (jsonNode.has("description")) {

            val descNode = jsonNode.get("description")

            if (descNode.isNull) {
                newDescription = null
            } else if (descNode.isTextual) {
                newDescription = descNode.asText()
            } else {
                return ResponseEntity.status(400).build()
            }
        }

        if (jsonNode.has("author")) {

            val authorNode = jsonNode.get("author")

            if (authorNode.isNull) {
                newAuthor = null
            } else if (authorNode.isTextual) {
                newAuthor = authorNode.asText()
            } else {
                return ResponseEntity.status(400).build()
            }
        }

        if (jsonNode.has("genre")) {

            val genreNode = jsonNode.get("genre")

            if (genreNode.isNull) {
                newGenre = null
            } else if (genreNode.isTextual) {
                newGenre = genreNode.asText()
            } else {
                return ResponseEntity.status(400).build()
            }
        }

        if (jsonNode.has("price")) {

            val priceNode = jsonNode.get("price")

            if (priceNode.isNull) {
                newPrice = null
            } else if (priceNode.isNumber) {
                newPrice = priceNode.intValue()
            } else {
                return ResponseEntity.status(400).build()
            }
        }

        if (jsonNode.has("rating")) {

            val ratingNode = jsonNode.get("rating")

            if (ratingNode.isNull) {
                newRating = null
            } else if (ratingNode.isNumber) {
                newRating = ratingNode.intValue()
            } else {
                return ResponseEntity.status(400).build()
            }
        }

        //now that the input is validated, do the update
        dto.name = newName
        dto.description = newDescription
        dto.author = newAuthor
        dto.genre = newGenre
        dto.price = newPrice
        dto.rating = newRating

        try {
            val successful = repo.updateBook(
                dto.name,
                dto.description,
                dto.genre,
                dto.author,
                dto.price,
                dto.rating,
                dto.id!!.toLong()
            )
            if (!successful) {
                return ResponseEntity.status(400).build()
            }
            return ResponseEntity.status(204).build()
        } catch (e: ConstraintViolationException) {
            return ResponseEntity.status(400).build()
        } finally {
            mergeBookTimer.stop()
        }

    }

    @ApiOperation("Delete book by id")
    @DeleteMapping(path = arrayOf("/{id}"))
    @ApiResponses(
        ApiResponse(code = 204, message = "No content, book successfully deleted"),
        ApiResponse(code = 400, message = "Id is not correct"),
        ApiResponse(code = 404, message = "Could not find book")
    )
    fun deleteBookById(
        @ApiParam("Id of book to delete")
        @PathVariable("id")
        pathId: Long?
    ): ResponseEntity<Any> {

        metrics.meter("books").mark()

        val id: Long
        try {
            id = pathId!!.toLong()
            repo.delete(id)
        } catch (e: NumberFormatException) {
            return ResponseEntity.status(400).build()
        } catch (e1: java.lang.Exception) {
            return ResponseEntity.status(404).build()
        }
        metrics.counter("books available").dec()

        metrics.register("books count", Gauge<Int> {
            repo.count().toInt()
        })

        return ResponseEntity.status(204).build()
    }


    private fun isDtoFieldsNotNull(bookDto: BookDto): Boolean {
        if (bookDto.name.isNullOrBlank() || bookDto.description.isNullOrBlank() || bookDto.genre.isNullOrBlank()
            || bookDto.author.isNullOrBlank() || bookDto.price == null || bookDto.price == 0 || bookDto.rating == null || bookDto.rating == 0 || bookDto.rating!! > 5
        ) {
            return false
        }

        return true

    }
}