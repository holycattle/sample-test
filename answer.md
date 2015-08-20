### Feedback/room for improvement:

1. Make use of standard error response codes; some of the tests expect the user to always return
200, which caused me some confusion.
2. Reduce all the boilerplate -- especially in writing models -- by writing macros and maybe
organizing stuff into inheritable and/or generic classes. Maybe even write DSPs?
3. Setup Slick Evolutions for version tracking the database; setup Slick code generation for tables
4. Reduce boilerplate when handling Futures. (It could be that I'm still new to Scala and therefore not aware of shortcuts and more idiomatic approaches yet, but I feel that I'm writing too much boilerplate stuff.)
5. Write support for Joda Dates (dates sure are a pain to work with lol). [EDIT: Found a plugin that works well with Slick. Hurrah!]
6. Add role-based authentication using decorators or something. I did my auth manually by querying the database.
7. Make querying easier, e.g. convenience functions for handling join queries done with list comprehensions (`for {}`).
8. Rewrite stuff that aren't idiomatic in Scala (this is my first non-trivial project in Scala LOL).
9. Come up with a better structure and naming convention for code.
10. Check if there are any synchronous calls that can become asynchronous instead.
11. Modify the database schema to use foreign keys.
12. Setup a database migration workflow for any schema changes (related to #3).
13. I put too much of the logic in Models.scala; perhaps I can refactor some of those into the controllers instead.
14. Break Models.scala into smaller files for easier readability.
