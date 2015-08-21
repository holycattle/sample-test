### Feedback/room for improvement:

1. Make use of standard error response codes; some of the tests expect the user to always return
200, which caused me some confusion.
2. Reduce all the boilerplate -- especially in writing models -- by writing macros and maybe
organizing stuff into inheritable and/or generic classes. Maybe even write DSPs?
3. Setup Slick Evolutions for version tracking the database; setup Slick code generation for tables
4. Reduce boilerplate when handling Futures. (It could be that I'm still new to Scala and therefore not aware of shortcuts and more idiomatic approaches yet, but I feel that I'm writing too much boilerplate stuff.)
5. Write support for Joda Dates (dates sure are a pain to work with lol). [EDIT: Found an
open-source plugin that works well with Slick. Hurrah!]
6. Add role-based authentication per route using decorators or something. I did my auth manually by querying the database.
7. Make querying easier, e.g. convenience functions for handling join queries done with list
comprehensions (`for {}`).
8. Rewrite stuff that aren't idiomatic in Scala (this is my first non-trivial project in Scala). My functional programming is also a bit rusty, so I'm sure there are a lot of things in my code that
could be further simplified (through functional programming patterns like currying).
9. Come up with a better structure and naming convention for code.
10. Check if there are any synchronous calls that can become asynchronous instead. [EDIT: I had to
use Await.result() because I didn't know how to flatten some things. ;_; I'm obviously going to have
to refactor those.]
11. Modify the database schema to use foreign keys; not using foreign keys is prone to errors (and
adds more complexity/dirty work in the code for checking if an entry exists. Some table data types
in the schema also aren't consistent -- some of the primary keys are Long while the pseudo-foreign
keys are Int.
12. Setup a database migration workflow for any schema changes (related to #3).
13. I think I put too much of the logic in Models.scala; perhaps I can refactor some of those into
the controllers instead. I should also break Models.scala and Application.scala into smaller files
for easier readability.
14. Automate whatever can be automated in setting up via Vagrant/Ansible.
15. Fix all the dirty hacks I did to save time and finish as soon as possible. T_T
