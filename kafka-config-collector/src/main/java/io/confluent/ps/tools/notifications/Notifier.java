package io.confluent.ps.tools.notifications;

import io.confluent.ps.tools.checks.Offense;
import net.wushilin.props.EnvAwareProperties;

import java.util.List;

public interface Notifier {
    void configure(EnvAwareProperties properties);
    void notify(List<Offense> offenses);
}
