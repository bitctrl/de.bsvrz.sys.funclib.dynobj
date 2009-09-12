/*
 * Funktionsbibliothek zum Arbeit mit dynamischen Objekten im Datenverteiler
 * Copyright (C) 2009 BitCtrl Systems GmbH 
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Contact Information:
 * BitCtrl Systems GmbH
 * Weissenfelser Strasse 67
 * 04229 Leipzig
 * Phone: +49 341-490670
 * mailto: info@bitctrl.de
 */

package de.bsvrz.sys.funclib.dynobj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.DataAndATGUsageInformation;
import de.bsvrz.dav.daf.main.DavConnectionListener;
import de.bsvrz.dav.daf.main.config.ConfigurationArea;
import de.bsvrz.dav.daf.main.config.ConfigurationChangeException;
import de.bsvrz.dav.daf.main.config.DynamicObject;
import de.bsvrz.dav.daf.main.config.DynamicObjectType;
import de.bsvrz.dav.daf.main.config.MutableSet;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Die Klasse beschreibt ein Objekt, mit dem die Verwaltung von dynamischen
 * Objekten innerhalb eines Datenverteilersystems vereinheitlicht wird.
 * Insbesondere wird hiermit die Verteilung von dynamischen Objekten auf
 * verschiedene Konfigurationsbereiche organisiert.
 * 
 * Die benutzerabh�ngige Zuordnung der dynamischen Objekte zu
 * Konfigurationsbereichen �ber ein Verwaltungsobjekt dieser Klasse, erfolgt
 * �ber einen Parameterdatensatz, der Bestandteil der AOE des Systems ist. Die
 * Attributgruppe "atg.verwaltungDynamischerObjekte" enth�lt ein variables Feld
 * von Attributlisten, die jeweils aus den Attributen "Objekttyp" (einer
 * Referenz auf den Typ eines dynamsichen Objekts) und "Konfigurationsbereich"
 * (einer Referenz auf einen Kobfigurationsbereich bestehen.
 * 
 * Beim Anlegen eines dynamischen Objekts �ber die hier bereitgestellten
 * Funktionen wird dieser Parametersatz ausgewertet und ermittelt, in welchem
 * Konfigurationsbereich das jeweils zu erzeugende Objekt abgelegt werden soll.
 * 
 * Wird innerhalb des Parameters keine entsprechende Zuordnung gefunden, wird
 * das Objekt entweder innerhalb des Default-Bereiches der AOE abgelegt oder das
 * Anlegen des Objekts schl�gt fehl. Das Verhalten kann f�r das
 * Verwaltungsobjekt per Funktionsaufruf definiert werden.
 * 
 * @author BitCtrl Systems GmbH, Uwe Peuker
 * @version $Id$
 */
public final class DynamischeObjekte implements DavConnectionListener {

	/** die Menge der angelegten Instanzen von Verwaltungsobjekten. */
	private static Map<ClientDavInterface, DynamischeObjekte> instanzen = new HashMap<ClientDavInterface, DynamischeObjekte>();

	/**
	 * Funktion zum Erzeugen eines Verwaltungsobjekts f�r dynamische Objekte.
	 * Die Instanzen der Objekte sind Singletons pro Datenverteilerverbindung
	 * und werden intern innerhalb der Klasse verwaltet.
	 * 
	 * Eine Instanz eines Verwaltungsobjektes wird aus der internen Verwaltung
	 * freigegeben, wenn die �bergebene Datenverteilerverbindung geschlossen
	 * wird. Die Datenverteilerverbindung, mit dem eine solche Instanz abgerufen
	 * wird sollte daher immer eine aktive Verbindung haben, ansonsten besteht
	 * hier die Gefahr eines Memory-Leaks.
	 * 
	 * Die Datenverteilerverbindung muss unbedingt �bergeben werden, der Wert
	 * <code>null</code> ist nicht zul�ssig.
	 * 
	 * @param dav
	 *            die Datenverteilerverbindung �ber die dynamische Objekte
	 *            verwaltet werden sollen
	 * 
	 * @return eine Instanz zur Verwaltung dynamischer Objekte
	 */
	public static DynamischeObjekte getInstanz(final ClientDavInterface dav) {
		assert (dav != null);

		DynamischeObjekte result = DynamischeObjekte.instanzen.get(dav);
		if (result == null) {
			result = new DynamischeObjekte(dav);
			DynamischeObjekte.instanzen.put(dav, result);
		}
		return result;
	}

	/** die verwendete Datenverteilerverbindung. */
	private final ClientDavInterface verbindung;

	/** die Verwaltung der Zuordnungen von Typ zu Konfigurationsbereich. */
	private final ZuordnungsVerwaltung zuordnung;

	/**
	 * Privater Konstruktor f�r eine Verwaltungsobjekt. Der Zugriff auf die
	 * Objekte erfolgt durch die Factory-Methode
	 * {@link #getInstanz(ClientDavInterface)}.
	 * 
	 * @param verbindung
	 *            die verwendete Datenverteilerverbindung
	 */
	private DynamischeObjekte(final ClientDavInterface verbindung) {
		assert (verbindung != null);
		this.verbindung = verbindung;
		verbindung.addConnectionListener(this);

		zuordnung = new ZuordnungsVerwaltung(verbindung);
	}

	/**
	 * die Funktion entfernt alle Elemente aus der �bergebenen Menge, die nicht
	 * mehr g�ltig sind.
	 * 
	 * @param menge
	 *            die Menge, die geleert werden soll
	 * @throws DynObjektException
	 *             die Menge konnte nicht bereinigt werden.
	 */
	public void bereinigeMenge(final MutableSet menge)
			throws DynObjektException {

		assert (menge != null);

		final Collection<SystemObject> elemente = new ArrayList<SystemObject>();

		for (final SystemObject element : elemente) {
			if (!element.isValid()) {
				elemente.add(element);
			}
		}
		if (elemente.size() > 0) {
			try {
				menge.remove(elemente
						.toArray(new SystemObject[elemente.size()]));
			} catch (final ConfigurationChangeException e) {
				Debug.getLogger().error(e.getLocalizedMessage());
				throw new DynObjektException(
						"Es konnten nicht alle ung�ltigen Elemente aus der Menge entfernt werden!",
						elemente);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Implemetiert das Interface {@link DavConnectionListener}.
	 * 
	 * Das der Datenverteilerverbindung zugeordnete Verwaltungsobjekt wird aus
	 * der internen Verwaltung entfernt, damit die Ressourcen vom GC freigegeben
	 * werden k�nnen.
	 */
	@Override
	public void connectionClosed(final ClientDavInterface connection) {
		DynamischeObjekte.instanzen.remove(verbindung);
	}

	/**
	 * die Funktion entfernt alle Elemente aus der �bergebenen Menge. Optional
	 * k�nnen die aus der Menge entfernten Objekte auch selbst entfernt werden.
	 * 
	 * Wenn das Entfernen der Elemente aus der Menge bereits fehlschl�gt, werden
	 * die Objekte auch nicht gel�scht.
	 * 
	 * @param menge
	 *            die Menge, die geleert werden soll
	 * @param loescheObjekte
	 *            <code>true</code>, wenn die aus der Menge entferneten Objekte
	 *            auch selbst entfernt werden sollen
	 * @throws DynObjektException
	 *             die Menge konnte nicht geleert werden oder das L�schen der
	 *             Objekte ist fehlgeschlagen.
	 */
	public void entferneAlleObjekteAusMenge(final MutableSet menge,
			final boolean loescheObjekte) throws DynObjektException {

		assert (menge != null);

		final Collection<SystemObject> elemente = new ArrayList<SystemObject>();
		elemente.addAll(menge.getElements());
		if (elemente.size() > 0) {
			try {
				menge.remove(elemente
						.toArray(new SystemObject[elemente.size()]));
			} catch (final ConfigurationChangeException e) {
				Debug.getLogger().error(e.getLocalizedMessage());
				throw new DynObjektException(
						"Es konnten nicht alle Elemente aus der Menge entfernt werden!",
						elemente);
			}

			if (loescheObjekte) {
				for (final SystemObject element : new ArrayList<SystemObject>(
						elemente)) {
					try {
						element.invalidate();
						elemente.remove(element);
					} catch (final ConfigurationChangeException e) {
						Debug.getLogger().error(e.getLocalizedMessage());
					}
				}

				if (elemente.size() > 0) {
					throw new DynObjektException(
							"Es konnten nicht alle Elemente gel�scht werden!",
							elemente);
				}
			}
		}
	}

	/**
	 * die Funktion entfernt das �bergebene Objekt aus der �bergebenen Menge.
	 * Optional kann �ber den Parameter <i>loescheObjekt</i> definiert werden,
	 * ob das Objekt selbst auch entfernt wird.
	 * 
	 * Als Ergebnis wird der Wert <code>true</code> geliefert, wenn das Element
	 * aus der Menge entfernt werden konnte, <code>false</code>, wenn es nicht
	 * in der Menge enthalten war.
	 * 
	 * Die Funktion bildet keine Transaktion, d.h. wenn beispielsweise das
	 * Entfernen des Objekts aus der Menge erfolgreich war, aber das Objekt
	 * selbst nicth gel�scht werden konnte, wird eine
	 * {@link ConfigurationChangeException} geworfen. Das Objekt ist jedoch
	 * trotzdem nicht mehr Bestandteil der Menge. Wenn das Entfernen des Objekts
	 * aus der Menge fehlschl�gt (au�er es ist nicht Bestandteil der Menge),
	 * wird das Objekt auch nicht entfernt.
	 * 
	 * Die Funktion ist nicht synchronisiert mit den Benachrichtigungen �ber
	 * Mengen�nderungen, d.h. es werden lediglich die direkten R�ckmeldungen aus
	 * den Aufrufen der Datenverteilerapplikationsfunktionen ausgewertet!
	 * 
	 * @param objekt
	 *            das Objekt, das aus der Menge entfernt werden soll
	 * @param menge
	 *            die Menge aus der ein Objekt entfernt werden soll
	 * @param loescheObjekt
	 *            definiert, ob das Objekt selbst ebenfalls gel�scht werden soll
	 * 
	 * @return <code>true</code>, wenn das Objekt entfernt wurde
	 * 
	 * @throws DynObjektException
	 *             das Objekt konnte nicht aus der Menge entfernt werden oder
	 *             das Objekt konnte nicht gel�scht werden
	 */
	public boolean entferneObjektAusMenge(final DynamicObject objekt,
			final MutableSet menge, final boolean loescheObjekt)
			throws DynObjektException {

		assert (menge != null);
		assert (objekt != null);

		boolean result = false;

		if (menge.getElements().contains(objekt)) {
			try {
				menge.remove(objekt);
			} catch (final ConfigurationChangeException e) {
				Debug.getLogger().error(e.getLocalizedMessage());
				throw new DynObjektException("Das Objekt: " + objekt
						+ " konnte nicht aus der Menge entfernt werden!",
						objekt);
			}

			if (loescheObjekt) {
				try {
					objekt.invalidate();
				} catch (final ConfigurationChangeException e) {
					Debug.getLogger().error(e.getLocalizedMessage());
					throw new DynObjektException("Das Element " + objekt
							+ " konnte nicht entfernt werden!", objekt);
				}
			}

			result = true;
		}

		return result;
	}

	/**
	 * die Funktion erzeugt ein dynamisches Objekt auf Basis der �bergebenen
	 * Daten.
	 * 
	 * @param typ
	 *            der Typ des zu erzeugenden Objekts
	 * @param name
	 *            der Name
	 * @param pid
	 *            die gew�nschte PID
	 * @return das erzeugte Objekt
	 * @throws DynObjektException
	 *             das Objekt konnte nicht in der Konfiguration angelegt werden
	 * 
	 * @see #erzeugeObjekt(DynamicObjectType, String, String, Collection)
	 * @see #erzeugeObjektInMenge(DynamicObjectType, String, String, MutableSet)
	 * @see #erzeugeObjektInMenge(DynamicObjectType, String, String, Collection,
	 *      MutableSet)
	 */
	public DynamicObject erzeugeObjekt(final DynamicObjectType typ,
			final String name, final String pid) throws DynObjektException {
		return erzeugeObjekt(typ, name, pid, null);
	}

	/**
	 * die Funktion erzeugt ein dynamisches Objekt auf Basis der �bergebenen
	 * Daten. Zus�tzlich zu den eigentlichen Objektinformationen k�nnen
	 * konfigurierende Datens�tze f�r das Objekt �bergeben werden.
	 * 
	 * @param typ
	 *            der Typ des zu erzeugenden Objekts
	 * @param name
	 *            der Name
	 * @param pid
	 *            die gew�nschte PID
	 * @param konfigurationsDaten
	 *            die konfigurierenden Datens�tze des Objekts
	 * 
	 * @return das erzeugte Objekt
	 * 
	 * @throws DynObjektException
	 *             das Objekt konnte nicht in der Konfiguration angelegt werden
	 * 
	 * @see #erzeugeObjekt(DynamicObjectType, String, String)
	 * @see #erzeugeObjektInMenge(DynamicObjectType, String, String, MutableSet)
	 * @see #erzeugeObjektInMenge(DynamicObjectType, String, String, Collection,
	 *      MutableSet)
	 */
	public DynamicObject erzeugeObjekt(final DynamicObjectType typ,
			final String name, final String pid,
			final Collection<DataAndATGUsageInformation> konfigurationsDaten)
			throws DynObjektException {
		if (zuordnung == null) {
			Debug
					.getLogger()
					.error(
							"Die Zuordnungstabelle zum Anlegen dynamischer Objekte ist nicht verf�gbar!");
			throw new DynObjektException(
					"Die Zuordnungstabelle zum Anlegen dynamischer Objekte ist nicht verf�gbar!");
		}

		final ConfigurationArea kb = zuordnung.getKonfigurationsBereich(typ);
		if (kb == null) {
			Debug.getLogger().error(
					"Es konnte kein Konfigurationsbereich zum Anlegen von Objekten des Typs: "
							+ typ + " ermittelt werden!");
			throw new DynObjektException(
					"Es konnte kein Konfigurationsbereich zum Anlegen von Objekten des Typs: "
							+ typ + " ermittelt werden!");
		}

		try {
			DynamicObject result = null;
			if (konfigurationsDaten == null) {
				result = kb.createDynamicObject(typ, pid, name);
			} else {
				result = kb.createDynamicObject(typ, pid, name,
						konfigurationsDaten);
			}

			return result;

		} catch (final ConfigurationChangeException e) {
			Debug.getLogger().error(e.getLocalizedMessage());
			throw new DynObjektException(
					"Das dynamische Objekt konnte nicht angelegt werden: "
							+ e.getLocalizedMessage());
		}
	}

	/**
	 * die Funktion erzeugt ein dynamisches Objekt auf Basis der �bergebenen
	 * Daten und tr�gt dieses in die �bergebene Menge ein. Zus�tzlich zu den
	 * eigentlichen Objektinformationen k�nnen optional konfiguriernde
	 * datens�tze beim Anlegen des Objekts �bergeben werden.
	 * 
	 * Es handelt sich nicht um eine Transaktion, d.h. wenn das Objekt angelegt
	 * und nicht in die Menge eingetragen werden konnte, bleibt das Objekt
	 * trotzdem bestehen und muss gegebenenfalls in der entsprechenden
	 * Fehlerbehandlungsroutine h�ndisch entfernt werden.
	 * 
	 * @param typ
	 *            der Typ des zu erzeugenden Objekts
	 * @param name
	 *            der Name
	 * @param pid
	 *            die gew�nschte PID
	 * @param konfigurationsDaten
	 *            die konfigurierenden Datens�tze des neu anzulegenden Objekts
	 * @param menge
	 *            die Menge in die das Objekt eingetragen werden soll
	 * 
	 * @return das erzeugte Objekt
	 * 
	 * @throws DynObjektException
	 *             das Objekt konnte nicht in der Konfiguration angelegt bzw. in
	 *             die Menge eingetragen werden.
	 * 
	 * @see #erzeugeObjekt(DynamicObjectType, String, String)
	 * @see #erzeugeObjekt(DynamicObjectType, String, String, Collection)
	 * @see #erzeugeObjektInMenge(DynamicObjectType, String, String, MutableSet)
	 * @see #erzeugeObjektInMenge(DynamicObjectType, String, String, MutableSet)
	 */
	public DynamicObject erzeugeObjektInMenge(final DynamicObjectType typ,
			final String name, final String pid,
			final Collection<DataAndATGUsageInformation> konfigurationsDaten,
			final MutableSet menge) throws DynObjektException {

		assert (menge != null);

		final DynamicObject objekt = erzeugeObjekt(typ, name, pid,
				konfigurationsDaten);
		try {
			menge.add(objekt);
			return objekt;
		} catch (final ConfigurationChangeException e) {
			Debug.getLogger().error(e.getLocalizedMessage());
			throw new DynObjektException(e.getLocalizedMessage(), objekt);
		}
	}

	/**
	 * die Funktion erzeugt ein dynamisches Objekt auf Basis der �bergebenen
	 * Daten und tr�gt dieses in die �bergebene Menge ein.
	 * 
	 * Es handelt sich nicht um eine Transaktion, d.h. wenn das Objekt angelegt
	 * und nicht in die Menge eingetragen werden konnte, bleibt das Objekt
	 * trotzdem bestehen und muss gegebenenfalls in der entsprechenden
	 * Fehlerbehandlungsroutine h�ndisch entfernt werden.
	 * 
	 * @param typ
	 *            der Typ des zu erzeugenden Objekts
	 * @param name
	 *            der Name
	 * @param pid
	 *            die gew�nschte PID
	 * @param menge
	 *            die Menge in die das Objekt eingetragen werden soll
	 * 
	 * @return das erzeugte Objekt
	 * 
	 * @throws DynObjektException
	 *             das Objekt konnte nicht in der Konfiguration angelegt bzw. in
	 *             die Menge eingetragen werden.
	 * 
	 * @see #erzeugeObjekt(DynamicObjectType, String, String)
	 * @see #erzeugeObjekt(DynamicObjectType, String, String, Collection)
	 *      MutableSet)
	 * @see #erzeugeObjektInMenge(DynamicObjectType, String, String, MutableSet)
	 */
	public DynamicObject erzeugeObjektInMenge(final DynamicObjectType typ,
			final String name, final String pid, final MutableSet menge)
			throws DynObjektException {
		return erzeugeObjektInMenge(typ, name, pid, null, menge);
	}

	/**
	 * die Funktion f�gt das �bergebene Objekt in die angegebene Menge ein.
	 * 
	 * @param objekt
	 *            das Objekt
	 * @param menge
	 *            die Menge
	 * @return <code>true</code>, wenn das Objekt noch nicht in der Menge
	 *         enthalten war und erfolgreich eingef�gt wurde; <code>false</code>
	 *         wenn das Objekt bereits Bestandteil der Menge war.
	 * @throws DynObjektException
	 *             das Objekt konnte nicht in die Menge eingef�gt werden.
	 */
	public boolean fuegeObjektInMengeEin(final DynamicObject objekt,
			final MutableSet menge) throws DynObjektException {
		assert (objekt != null);
		assert (menge != null);

		boolean result = false;

		try {
			if (!menge.getElements().contains(objekt)) {
				result = true;
				menge.add(objekt);
			}
		} catch (final ConfigurationChangeException e) {
			Debug.getLogger().error(e.getLocalizedMessage());
			throw new DynObjektException(e.getLocalizedMessage(), objekt);
		}

		return result;
	}

	/**
	 * die Funktion liefert den Konfigurationsbereich, der dem �bergebenen
	 * Objekttyp zugeordnet ist. Wurde keiner gefunden, wird der
	 * Standardkonfigurationsbereich der AOE geliefert.
	 * 
	 * @param typ
	 *            der Typ f�r ein dynamisches Objekt
	 * @return der ermittelte Konfigurationsbereich
	 */
	public ConfigurationArea getKonfigurationsBereich(
			final DynamicObjectType typ) {
		return zuordnung.getKonfigurationsBereich(typ);
	}

	/**
	 * die Funktion l�scht alle Objekte mit dem �bergebenen Typ, die sich nicht
	 * in einer der �bergebenen Mengen befinden.
	 * 
	 * @param typ
	 *            der Typ dessen Instanzen entfernt werden sollen
	 * @param mengen
	 *            die Mengen in denen die nicht zu entfernenden Objekte
	 *            enthalten sind
	 * @throws DynObjektException
	 *             die Objekte konnten nicht oder nicht vollst�ndig entfernt
	 *             werden
	 */
	public void loescheAlleNichtZugeordnetenObjekte(
			final DynamicObjectType typ, final MutableSet... mengen)
			throws DynObjektException {

		assert (typ != null);

		final Set<SystemObject> mengenObjekte = new HashSet<SystemObject>();

		if (mengen != null) {
			for (final MutableSet menge : mengen) {
				mengenObjekte.addAll(menge.getElements());
			}
		}

		final Collection<SystemObject> liste = new ArrayList<SystemObject>();
		final Collection<SystemObject> fehlerListe = new ArrayList<SystemObject>();
		liste.addAll(typ.getElements());

		for (final SystemObject objekt : liste) {
			if (!mengenObjekte.contains(objekt)) {
				try {
					objekt.invalidate();
				} catch (final ConfigurationChangeException e) {
					Debug.getLogger().error(e.getLocalizedMessage());
					fehlerListe.add(objekt);
				}
			}
		}

		if (fehlerListe.size() > 0) {
			throw new DynObjektException(
					"Es konnten nicht alle Objekte gel�scht werden!",
					fehlerListe.toArray(new SystemObject[fehlerListe.size()]));
		}
	}

	/**
	 * die Funktion l�scht alle Objekte mit dem �bergebenen Typ.
	 * 
	 * @param typ
	 *            der Typ dessen Instanzen entfernt werden sollen
	 * @throws DynObjektException
	 *             die Objekte konnten nicht oder nicht vollst�ndig entfernt
	 *             werden
	 */
	public void loescheAlleObjekte(final DynamicObjectType typ)
			throws DynObjektException {

		loescheAlleNichtZugeordnetenObjekte(typ, (MutableSet[]) null);
	}

	/**
	 * die Funktion l�scht das �bergebene dynamische Objekt. Die Funktion ist
	 * zwar eigentlich �berfl�ssig, da sie am Ende lediglich
	 * {@link SystemObject#invalidate()} aufruft, vervollst�ndigt aber die
	 * Funktionalit�t des Verwaltungsobjektes.
	 * 
	 * @param objekt
	 *            das Objekt, das gel�scht werden soll
	 * @throws DynObjektException
	 *             das Objekt konnte nicht gel�scht werden
	 */
	public void loescheObjekt(final DynamicObject objekt)
			throws DynObjektException {

		assert (objekt != null);

		try {
			objekt.invalidate();
		} catch (final ConfigurationChangeException e) {
			Debug.getLogger().error(e.getLocalizedMessage());
			throw new DynObjektException("Das Objekt " + objekt
					+ " konnte nicht gel�scht werden!", objekt);
		}
	}
}
