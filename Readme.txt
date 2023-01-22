Anton Reut, s24382

Wybrałem TCP jako protokuł komunikacji.
Stworzyłem swoją klasę DatabaseRecord dla przechowywania Recordów. Ona posiada tylko dwa pola (key, value) i jeszcze metody get, set i toString.
DatabaseNode:
	Przechowam sąsiedzi tego Noda w ArrayList, żeby o nich pamiętać i z nimi się komunikować(kasuję przy potrzebie).
	Przechowam za pomocą TreeSet<UUID> (Uniwersalny unikalny identyfikator) identyfikowatory operacyj, żeby Nody nie komunikowali się nieskończono. 
	Jeżeli ten Node już posiada ten ID, to oznacza, że ta operacja już przechodziła przez ten Node.
Działanie:
	DatabaseNode:
Najperw analizuję to, co przyszło w argumentach. Na podstawie tego uzupełniam tcpPort (tworzonego serwera), record(tego Node), tworzę Queue<DatabaseNode> toConnect.
Potym działam.
Najperw robię connect do wszystkich Nodeów z toConnect wysyłając operację "newnode <port>".
Potym tworze serverSocket i czekam na server.accept(), jeżeli zakceptowało to tworzę nowy wątek NodeThread.
	DatabaseThread:
Najperw odzytuje napis od klienta, analizuję wartości. Jeżeli mi nie przyszedł id, to tworzę nową. W switch(case) pytam o operację i robię czego potrzebuje operacja.
Komunikacji między Nodami mają taką postać: operacja + " " + argument + " " + id.

