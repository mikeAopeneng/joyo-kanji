/****************************************************************************************
 * Copyright (c) 2013 Bibek Shrestha <bibekshrestha@gmail.com>                          *
 * Copyright (c) 2013 Zaur Molotnikov <qutorial@gmail.com>                              *
 * Copyright (c) 2013 Nicolas Raoul <nicolas.raoul@gmail.com>                           *
 * Copyright (c) 2013 Flavio Lerda <flerda@gmail.com>                                   *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package website.openeng.anki.multimediacard.impl;

import website.openeng.anki.multimediacard.IMultimediaEditableNote;
import website.openeng.anki.multimediacard.fields.IField;

import java.util.ArrayList;

/**
 * Implementation of the editable note.
 * <p>
 * Has to be translate to and from anki db format.
 */

public class MultimediaEditableNote implements IMultimediaEditableNote {
    private static final long serialVersionUID = -6161821367135636659L;
    boolean mIsModified = false;

    ArrayList<IField> mFields;
    private long mModelId;


    public void circularSwap() {
        if (mFields == null) {
            return;
        }

        if (mFields.size() <= 1) {
            return;
        }

        ArrayList<IField> newFields = new ArrayList<IField>();
        newFields.add(mFields.get(mFields.size() - 1));
        newFields.addAll(mFields);
        newFields.remove(mFields.size());

        mFields = newFields;
    }


    void setThisModified() {
        mIsModified = true;
    }


    @Override
    public boolean isModified() {
        return mIsModified;
    }


    // package
    public void setNumFields(int numberOfFields) {
        getFieldsPrivate().clear();
        for (int i = 0; i < numberOfFields; ++i) {
            getFieldsPrivate().add(null);
        }
    }


    private ArrayList<IField> getFieldsPrivate() {
        if (mFields == null) {
            mFields = new ArrayList<IField>();
        }

        return mFields;
    }


    @Override
    public int getNumberOfFields() {
        return getFieldsPrivate().size();
    }


    @Override
    public IField getField(int index) {
        if (index >= 0 && index < getNumberOfFields()) {
            return getFieldsPrivate().get(index);
        }
        return null;
    }


    @Override
    public boolean setField(int index, IField field) {
        if (index >= 0 && index < getNumberOfFields()) {
            // If the same unchanged field is set.
            if (getField(index) == field) {
                if (field.isModified()) {
                    setThisModified();
                }
            } else {
                setThisModified();
            }

            getFieldsPrivate().set(index, field);

            return true;
        }
        return false;
    }


    public void setModelId(long modelId) {
        mModelId = modelId;
    }


    public long getModelId() {
        return mModelId;
    }

}
