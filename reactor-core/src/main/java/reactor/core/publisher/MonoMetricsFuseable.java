/*
 * Copyright (c) 2011-2018 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.core.publisher;

import java.util.List;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.publisher.MonoMetrics.MicrometerMonoMetricsFuseableSubscriber;
import reactor.util.Metrics;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuple2;

/**
 * Activate metrics gathering on a {@link Mono} (Fuseable version), assumes Micrometer is
 * on the classpath.
 *
 * @implNote Metrics.isInstrumentationAvailable() test should be performed BEFORE instantiating
 * or referencing this class, otherwise a {@link NoClassDefFoundError} will be thrown if
 * Micrometer is not there.
 *
 * @author Simon Baslé
 */
final class MonoMetricsFuseable<T> extends MonoOperator<T, T> implements Fuseable {

	final String    name;
	final List<Tag> tags;

	@Nullable
	final MeterRegistry registryCandidate;

	MonoMetricsFuseable(Mono<? extends T> mono) {
		this(mono, null);
	}

	/**
	 * For testing purposes.
	 *
	 * @param registry the registry to use, as a plain {@link Object} to avoid leaking dependency
	 */
	MonoMetricsFuseable(Mono<? extends T> mono, @Nullable MeterRegistry registry) {
		super(mono);

		Tuple2<String, List<Tag>> nameAndTags = FluxMetrics.resolveNameAndTags(mono);
		this.name = nameAndTags.getT1();
		this.tags = nameAndTags.getT2();

		if (registry == null) {
			this.registryCandidate = (MeterRegistry) Metrics.getUnsafeRegistry();
		}
		else {
			this.registryCandidate = registry;
		}
	}

	@Override
	public void subscribe(CoreSubscriber<? super T> actual) {
		source.subscribe(new MicrometerMonoMetricsFuseableSubscriber<>(actual, registryCandidate,
				Clock.SYSTEM, this.name, this.tags));
	}

}
