import { ListTrainerWorkoutsQueryDto } from './list-trainer-workouts-query.dto';

/** Query for `GET .../workouts` (same semantics as `GET .../trainers/workouts`). */
export class ListWorkoutsQueryDto extends ListTrainerWorkoutsQueryDto {}
