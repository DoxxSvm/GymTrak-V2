import { PERMISSIONS_KEY } from '../../common/decorators/require-permissions.decorator';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import { PlansController } from './plans.controller';

describe('PlansController permissions', () => {
  const methodPermissions = (methodName: keyof PlansController) =>
    Reflect.getMetadata(PERMISSIONS_KEY, PlansController.prototype[methodName]);

  it('requires members permission for plan management routes', () => {
    expect(methodPermissions('list')).toEqual([PERMISSION_CODES.MEMBERS]);
    expect(methodPermissions('create')).toEqual([PERMISSION_CODES.MEMBERS]);
    expect(methodPermissions('createCompat')).toEqual([
      PERMISSION_CODES.MEMBERS,
    ]);
    expect(methodPermissions('enrolled')).toEqual([PERMISSION_CODES.MEMBERS]);
    expect(methodPermissions('getOne')).toEqual([PERMISSION_CODES.MEMBERS]);
    expect(methodPermissions('update')).toEqual([PERMISSION_CODES.MEMBERS]);
    expect(methodPermissions('updateCompat')).toEqual([
      PERMISSION_CODES.MEMBERS,
    ]);
    expect(methodPermissions('remove')).toEqual([PERMISSION_CODES.MEMBERS]);
  });

  it('leaves compat validation and member assignment to service-level checks', () => {
    expect(methodPermissions('validatePlan')).toBeUndefined();
    expect(methodPermissions('assignToMember')).toBeUndefined();
  });
});
