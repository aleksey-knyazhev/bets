import { useEffect, useState } from "react";

import { getRound } from "../api/roundApi";

export function useRound() {
  const [round, setRound] = useState<number | null>(null);

  useEffect(() => {
    let isMounted = true;

    getRound()
      .then((value) => {
        if (isMounted) {
          setRound(value);
        }
      })
      .catch(() => {
        if (isMounted) {
          setRound(1);
        }
      });

    return () => {
      isMounted = false;
    };
  }, []);

  return [round, setRound] as const;
}
