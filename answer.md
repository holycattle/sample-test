### Feedback/room for improvement:

##### Feedback for the test
1. Make use of standard HTTP error response codes; most of the tests expect the code to always
return 200, which caused me some confusion, since I tried to follow REST/HTTP standards.
2. Modify the database schema to use foreign keys; not using foreign keys is prone to errors (and
adds more complexity/dirty work in the code for checking if an entry exists. Some table data types
in the schema also aren't consistent -- some of the primary keys are Long while the pseudo-foreign
keys are Int.
3. The email field should be unique, although I do understand that the database schema was meant to
provide a convenient testing environment. (related to #2)
4. The specs for the REST endpoints can also be refactored to make more semantic sense and
be more conformant to REST standards, e.g. `POST /api/events/reserve` can be `POST /api/events/:id/(reserve|unreserve)`, or `POST /api/companies/events` can be `GET /api/companies/:id/events` instead.

##### Stuff to improve in my code
1. Reduce all the boilerplate -- especially in writing models -- by writing macros and maybe
organizing stuff into inheritable and/or generic classes. Maybe even write DSPs?
2. Setup Slick Evolutions for version tracking the database; setup Slick code generation for tables
3. Reduce boilerplate when handling Futures. (It could be that I'm still new to Scala and therefore not aware of shortcuts and more idiomatic approaches yet, but I feel that I'm writing too much boilerplate stuff.)
4. Add role-based authentication per route using decorators or something. I did my auth manually by querying the database.
5. Make querying easier, e.g. convenience functions for handling join queries done with list
comprehensions (`for {}`).
6. Rewrite stuff that aren't idiomatic in Scala (this is my first non-trivial project in Scala). My functional programming is also a bit rusty, so I'm sure there are a lot of things in my code that
could be further simplified (through functional programming patterns like currying).
7. Come up with a better structure and naming convention for code.
8. Check if there are any synchronous calls that can become asynchronous instead. [EDIT: I had to
use Await.result() because I didn't know how to flatten some things. ;_; I'm obviously going to have
to refactor those.]
9. Setup a database migration workflow for any schema changes (related to #3).
10. I think I put too much of the logic in Models.scala; perhaps I can refactor some of those into
the controllers instead. I should also break Models.scala and Application.scala into smaller files
for easier readability.
11. Automate whatever can be automated in setting up via Vagrant/Ansible.
12. Fix all the dirty hacks I did to save time and finish as soon as possible. T_T
