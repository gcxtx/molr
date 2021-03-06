/*
 * © Copyright 2016 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file “COPYING“. In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */

package cern.molr.commons.annotations;

/**
 * Interface use when generating source classes. {@link SourceMeProcessor}
 *
 * @author mgalilee
 */
public interface Source {
    /**
     * @return the source representation of a class encoded in base64
     */
    String base64Value();
}
