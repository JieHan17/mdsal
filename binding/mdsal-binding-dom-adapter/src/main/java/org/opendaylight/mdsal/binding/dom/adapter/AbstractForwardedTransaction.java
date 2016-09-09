/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.mdsal.binding.dom.adapter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.mdsal.common.api.AsyncTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.yangtools.concepts.Delegator;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.util.concurrent.MappingCheckedFuture;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

abstract class AbstractForwardedTransaction<T extends AsyncTransaction<YangInstanceIdentifier, NormalizedNode<?, ?>>>
        implements Delegator<T>, Identifiable<Object> {

    private final T delegate;
    private final BindingToNormalizedNodeCodec codec;

    AbstractForwardedTransaction(final T delegateTx, final BindingToNormalizedNodeCodec codec) {
        this.delegate = Preconditions.checkNotNull(delegateTx, "Delegate must not be null");
        this.codec = Preconditions.checkNotNull(codec, "Codec must not be null");
    }


    @Override
    public final  Object getIdentifier() {
        return delegate.getIdentifier();
    }

    @Override
    public final  T getDelegate() {
        return delegate;
    }

    @SuppressWarnings("unchecked")
    protected final <S extends AsyncTransaction<YangInstanceIdentifier, NormalizedNode<?, ?>>> S getDelegateChecked(
            final Class<S> txType) {
        Preconditions.checkState(txType.isInstance(delegate));
        return (S) delegate;
    }

    protected final BindingToNormalizedNodeCodec getCodec() {
        return codec;
    }

    protected final <D extends DataObject> CheckedFuture<Optional<D>,ReadFailedException> doRead(
            final DOMDataTreeReadTransaction readTx, final LogicalDatastoreType store,
            final InstanceIdentifier<D> path) {
        Preconditions.checkArgument(!path.isWildcarded(), "Invalid read of wildcarded path %s", path);

        return MappingCheckedFuture.create(
                    Futures.transform(readTx.read(store, codec.toYangInstanceIdentifierBlocking(path)),
                                      codec.deserializeFunction(path)),
                    ReadFailedException.MAPPER);
    }
}
