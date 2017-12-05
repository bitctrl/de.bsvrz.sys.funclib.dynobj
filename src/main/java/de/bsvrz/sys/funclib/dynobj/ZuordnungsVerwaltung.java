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

import java.util.HashMap;
import java.util.Map;

import de.bsvrz.dav.daf.main.ClientDavInterface;
import de.bsvrz.dav.daf.main.ClientReceiverInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.Data.Array;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.DataState;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.ConfigurationArea;
import de.bsvrz.dav.daf.main.config.ConfigurationAuthority;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.DynamicObjectType;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * Die Klasse verwaltet die Zuordnungen von Typen dynamischer Objekte zu den
 * Konfigurationsbereichen, in den sie angelegt werden sollen.
 *
 * Es wird der Parameterdatensatz "atg.verwaltungDynamischerObjekte" an der
 * aktuellen AOE ausgelesen und beobachtet. Wenn der Parameterdatensatz keine
 * Zuordnung für einen gewüschten Objekttyp enthält, wird der in den
 * konfigurierenden Eigenschaften der AOE definierte
 * Standardkonfigurationsbereich als Ziel für dynamsiche Objekte geliefert.
 *
 * @author BitCtrl Systems GmbH, Uwe Peuker
 * @version $Id$
 */
class ZuordnungsVerwaltung implements ClientReceiverInterface {

	/**
	 * die Datenbeschreibung für den Zugriff auf den
	 * Zuordnungsparameterdatensatz.
	 */
	private final DataDescription desc;

	/** der Typ des Typs "Dynamisches Objekt" als Rückfallebene. */
	private final DynamicObjectType typDynamischesObjekt;

	/** der Standard-Konfigurationsbereich der AOE. */
	private final ConfigurationArea defaultBereich;

	/**
	 * die Zuordnungstabelle von dynamischen Objekttypen zu
	 * Konfigurationsbereichen.
	 */
	private final Map<DynamicObjectType, ConfigurationArea> zuordnungsTabelle = new HashMap<>();

	/**
	 * Konstruktor. Es wird eine Instanz der Zuordnungsverwaltung für die
	 * übergebene Datenverteilerverbindung erzeugt.
	 *
	 * @param verbindung
	 *            die Datenverteilerverbindung
	 */
	public ZuordnungsVerwaltung(final ClientDavInterface verbindung) {

		assert verbindung != null;

		final DataModel model = verbindung.getDataModel();

		final ConfigurationAuthority aoe = verbindung
				.getLocalConfigurationAuthority();

		final Data configData = aoe
				.getConfigurationData(model
						.getAttributeGroup("atg.konfigurationsVerantwortlicherEigenschaften"));
		final Array array = configData.getArray("defaultBereich");
		if (array.getLength() > 0) {
			defaultBereich = (ConfigurationArea) model.getObject(array
					.getTextValue(0).getText());
		} else {
			throw new IllegalStateException(
					"Der Defaultbereich des aktuellen Konfigurationsverantwortlichen konnte nicht ermittelt werden");
		}

		typDynamischesObjekt = (DynamicObjectType) model
				.getObject("typ.dynamischesObjekt");

		final AttributeGroup atg = model
				.getAttributeGroup("atg.verwaltungDynamischerObjekte");
		final Aspect aspect = model.getAspect("asp.parameterSoll");

		if ((atg == null) || (aspect == null)) {
			Debug.getLogger()
			.error("Der Parameterdatensatz für die Verwaltung dynamischer Objekte ist nicht verfügbar!");
			desc = null;
		} else {
			desc = new DataDescription(atg, aspect);
			final ResultData daten = verbindung.getData(aoe, desc, 0L);
			update(new ResultData[] { daten });
			verbindung.subscribeReceiver(this, aoe, desc,
					ReceiveOptions.normal(), ReceiverRole.receiver());
		}
	}

	/**
	 * die Funktion liefert den Konfigurationsbereichm der dem übergebenen
	 * Objekttyp zugeordnet ist. Wurde keiner gefunden, wird der Wert null
	 * geliefert. Eine Ausnahme besteht für den Fall, dass der
	 * Parameterdatensatz in älteren Systemen an der AOE nicht konfiguriert ist.
	 * In diesem Fall wird der erste konfigurierte Standardkonfigurationsbereich
	 * der AOE geliefert.
	 *
	 * @param typ
	 *            der Typ für ein dynamisches Objekt
	 * @return der ermittelte Konfigurationsbereich
	 */
	public ConfigurationArea getKonfigurationsBereich(
			final DynamicObjectType typ) {
		synchronized (zuordnungsTabelle) {
			ConfigurationArea result = zuordnungsTabelle.get(typ);
			if (result == null) {
				if (desc == null) {
					result = defaultBereich;
				} else {
					result = zuordnungsTabelle.get(typDynamischesObjekt);
				}
			}

			return result;
		}
	}

	/** {@inheritDoc} */
	@Override
	public void update(final ResultData[] results) {
		synchronized (zuordnungsTabelle) {
			zuordnungsTabelle.clear();
			for (final ResultData result : results) {
				final DataState state = result.getDataState();
				if (state == DataState.DATA) {
					final Data daten = result.getData();
					if (daten != null) {
						final Array array = daten
								.getArray("ZuordnungDynamischerObjektTypZuKB");
						for (int idx = 0; idx < array.getLength(); idx++) {
							SystemObject systemObject = array
									.getItem(idx)
									.getReferenceValue("DynamischerTypReferenz")
									.getSystemObject();

							if (!(systemObject instanceof DynamicObjectType)) {
								throw new IllegalStateException(
										this.getClass().getName()
										+ ": Der Parameterdatensatz "
										+ idx
										+ " für die Verwaltung dynamischer Objekte ist ungültig!");
							}

							final DynamicObjectType typ = (DynamicObjectType) array
									.getItem(idx)
									.getReferenceValue("DynamischerTypReferenz")
									.getSystemObject();

							systemObject = array
									.getItem(idx)
									.getReferenceValue(
											"KonfigurationsBereichReferenz")
											.getSystemObject();

							if (!(systemObject instanceof ConfigurationArea)) {
								throw new IllegalStateException(
										this.getClass().getName()
										+ ": Der Parameterdatensatz "
										+ idx
										+ " für die Verwaltung dynamischer Objekte ist ungültig!");
							}

							final ConfigurationArea kb = (ConfigurationArea) array
									.getItem(idx)
									.getReferenceValue(
											"KonfigurationsBereichReferenz")
											.getSystemObject();
							zuordnungsTabelle.put(typ, kb);
						}
					} else {
						Debug.getLogger().warning(
								"Der Datensatz enthält keine Daten: " + result);
					}
				} else {
					Debug.getLogger().warning(
							"Datensatz kann nicht ausgewertet werden: "
									+ result);
				}
			}
		}
	}
}
