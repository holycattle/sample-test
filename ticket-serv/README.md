# ticket-serv
A simple ticket/event reservation API and toy project created to experiment with Play 2.4, Scala, and Slick.

## Installation
After running `vagrant up` from the root directory of the project, do the following:
```bash
vagrant provision
vagrant ssh
mysql -uvagrant -pvagrant -Dvagrant < scala-test/sql/create.sql
cd scala-test
npm install
cd ticket-serv
activator
```
