Versionsverlauf
=============================================

## Version 1.0.2

Funktion "bereinigeMenge" verwendet die Elemente aus der Menge.

## Version 1.0.1

Anpassung des Fehlerverhaltens bezüglich des Parameterdatensatzes zur Verwaltung 
der Zuordnung "Dynamisches Objekt <--> Konfigurationsbereich"

- bei nicht konfiguriertem Parameterdatensatz wird der erste konfigurierte 
  Standardkonfigurationsbereich der AOE geliefert
- ist der Parameterdatensatz konfiguriert, muss auch ein entsprechender 
  Zuordnungsparameter parametriert sein. Als Rückfallebene kann bei der 
  Zuordnung der Typ "typ.dynamischesObjekt" verwendet werden. 

