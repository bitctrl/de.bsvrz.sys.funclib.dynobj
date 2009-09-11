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
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.DataState;
import de.bsvrz.dav.daf.main.ReceiveOptions;
import de.bsvrz.dav.daf.main.ReceiverRole;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.Data.Array;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.ConfigurationArea;
import de.bsvrz.dav.daf.main.config.ConfigurationAuthority;
import de.bsvrz.dav.daf.main.config.DataModel;
import de.bsvrz.dav.daf.main.config.DynamicObjectType;
import de.bsvrz.sys.funclib.debug.Debug;

/**
 * @author BitCtrl Systems GmbH, Uwe Peuker
 * @version $Id$
 */
class ZuordnungsVerwaltung implements ClientReceiverInterface {

	/**
	 * die Datenbeschreibung für den Zugriff auf den
	 * Zuordnungsparameterdatensatz.
	 */
	private final DataDescription desc;

	private final ConfigurationArea defaultBereich;

	Map<DynamicObjectType, ConfigurationArea> zuordnungsTabelle = new HashMap<DynamicObjectType, ConfigurationArea>();

	private final ClientDavInterface verbindung;

	public ZuordnungsVerwaltung(final ClientDavInterface verbindung) {

		this.verbindung = verbindung;
		DataModel model = verbindung.getDataModel();

		ConfigurationAuthority aoe = verbindung
				.getLocalConfigurationAuthority();

		defaultBereich = aoe.getConfigurationArea();

		AttributeGroup atg = model
				.getAttributeGroup("atg.verwaltungDynamischerObjekte");
		Aspect aspect = model.getAspect("asp.parameterSoll");

		if ((atg == null) || (aspect == null)) {
			desc = null;
		} else {
			desc = new DataDescription(atg, aspect);
			ResultData daten = verbindung.getData(aoe, desc, 0L);
			update(new ResultData[] { daten });
			verbindung.subscribeReceiver(this, aoe, desc, ReceiveOptions
					.normal(), ReceiverRole.receiver());
		}
	}

	ConfigurationArea getKonfigurationsBereich(final DynamicObjectType typ) {
		synchronized (zuordnungsTabelle) {
			ConfigurationArea result = zuordnungsTabelle.get(typ);
			if (result == null) {
				result = defaultBereich;
			}

			return result;
		}
	}

	@Override
	public void update(final ResultData[] results) {
		synchronized (zuordnungsTabelle) {
			zuordnungsTabelle.clear();
			for (ResultData result : results) {
				DataState state = result.getDataState();
				if (state == DataState.DATA) {
					Data daten = result.getData();
					if (daten != null) {
						Array array = daten
								.getArray("ZuordnungDynamischerObjektTypZuKB");
						for (int idx = 0; idx < array.getLength(); idx++) {
							DynamicObjectType typ = (DynamicObjectType) array
									.getItem(idx).getReferenceValue(
											"DynamischerTypReferenz")
									.getSystemObject();
							ConfigurationArea kb = (ConfigurationArea) array
									.getItem(idx).getReferenceValue(
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
