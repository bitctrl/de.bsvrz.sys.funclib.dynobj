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

import java.util.Collection;

import de.bsvrz.dav.daf.main.config.SystemObject;

/**
 * @author BitCtrl Systems GmbH, Uwe Peuker
 * @version $Id$
 */
public class ObjektLoeschenException extends DynObjektElementException {

	public ObjektLoeschenException(final String meldung,
			final Collection<SystemObject> elemente) {
		super(meldung, elemente);
		// TODO Auto-generated constructor stub
	}

	public ObjektLoeschenException(final String meldung,
			final SystemObject... elemente) {
		super(meldung, elemente);
	}
}
