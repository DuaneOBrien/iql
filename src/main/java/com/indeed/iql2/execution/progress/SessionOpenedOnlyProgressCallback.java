/*
 * Copyright (C) 2018 Indeed Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.indeed.iql2.execution.progress;

import com.google.common.base.Optional;
import com.indeed.imhotep.Shard;
import com.indeed.imhotep.api.ImhotepSession;
import com.indeed.iql2.execution.Session;
import com.indeed.iql2.execution.commands.Command;

import java.util.List;
import java.util.Map;

public class SessionOpenedOnlyProgressCallback implements ProgressCallback {
    private final ProgressCallback wrapped;

    public SessionOpenedOnlyProgressCallback(ProgressCallback wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void startSession(Optional<Integer> numCommands) {
    }

    @Override
    public void preSessionOpen(final Map<String, List<Shard>> datasetToChosenShards) {
    }

    @Override
    public void sessionOpened(ImhotepSession session) {
        wrapped.sessionOpened(session);
    }

    @Override
    public void sessionsOpened(Map<String, Session.ImhotepSessionInfo> sessions) {
    }

    @Override
    public void startCommand(Session session, Command command, boolean streamingToTSV) {
    }

    @Override
    public void endCommand(Session session, Command command) {
    }
}