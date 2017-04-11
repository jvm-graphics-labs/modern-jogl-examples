package glNext

/**
 * Created by GBarbieri on 11.04.2017.
 */

class Person private constructor(val name: String, val surname: String, val age: Int) {

    private constructor(builder: Builder) : this(builder.name, builder.surname, builder.age)

    companion object {
        fun create(init: Builder.() -> Unit) = Builder(init).build()
    }

    class Builder private constructor() {

        constructor(init: Builder.() -> Unit) : this() {
            init()
        }

        lateinit var name: String
        lateinit var surname: String
        var age: Int = 0

        fun name(init: Builder.() -> String) = apply { name = init() }

        fun surname(init: Builder.() -> String) = apply { surname = init() }

        fun age(init: Builder.() -> Int) = apply { age = init() }

        fun build() = Person(this)
    }
}

fun main(args: Array<String>) {
    Person.create {
        name { "Peter" }
        surname { "Slesarew" }
        age { 28 }
    }

    // OR

    Person.create {
        name = "Peter"
        surname = "Slesarew"
        age = 28
    }
}