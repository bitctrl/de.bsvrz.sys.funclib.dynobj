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

import de.bsvrz.dav.daf.main.config.SystemObject;

/**
 * Eine Exception, die auftreten kann, wenn dynamische Objekte mit Hilfe eines
 * Verwaltungsobjekts von Typ {@link DynamischeObjekte} angelegt und/oder
 * entfernt bzw. in Mengen eingetragen/entfernt werden sollen.
 * 
 * Die Exception kann eine Liste der Objekte, die von der Exception betroffen
 * sind enthalten.
 * 
 * @author BitCtrl Systems GmbH, Uwe Peuker
 * @version $Id$
 */
public class DynObjektException extends Exception {

	/** Versions-ID für die Serialisierung der Klasse. */
	private static final long serialVersionUID = 1L;

	/** die Liste der Objekte, die von der Exception betroffen sind. */
	final Collection<SystemObject> elementListe = new ArrayList<SystemObject>();

	/**
	 * Konstruktor mit Angabe eines beschreibenden Textes.
	 * 
	 * @param meldung
	 *            der beschreibende Text
	 */
	public DynObjektException(final String meldung) {
		super(meldung);
	}

	/**
	 * Konstruktor, der den Meldungstext und eine Collection der betroffenen
	 * Elemente übernimmt.
	 * 
	 * @param meldung
	 *            die Meldung
	 * @param elemente
	 *            die Elemente
	 */
	public DynObjektException(final String meldung,
			final Collection<SystemObject> elemente) {
		super(meldung);
		elemente.addAll(elemente);
	}

	/**
	 * Konstruktor, der den Meldungstext und ein Array der betroffenen Elemente
	 * übernimmt.
	 * 
	 * @param meldung
	 *            die Meldung
	 * @param elemente
	 *            die Elemente
	 */
	public DynObjektException(final String meldung,
			final SystemObject... elemente) {
		super(meldung);
		for (final SystemObject element : elemente) {
			elementListe.add(element);
		}
	}

	/**
	 * liefert eine Liste der von der Exception betroffenen Elemente.
	 * 
	 * @return die Liste
	 */
	public Collection<SystemObject> getElementListe() {
		return elementListe;
	}
}
