# /session-check
Verify a recorded session pulled from the phone.

1. Ask me for the session folder name, or list `adb shell run-as com.sensorlab ls files/sessions/` and ask which one.
2. Pull it: `adb exec-out run-as com.sensorlab tar c files/sessions/<id> > session.tar`, then extract into `./data/<id>/`.
3. Write or run a Python script `analysis/validate_session.py` that checks: t_ns strictly increasing per sensor, column counts match headers, effective Hz vs requested Hz, gaps larger than 5x the median interval, and session.json status.
4. Report a short table per sensor: samples, effective Hz, max gap, dropped estimate, problems found.
