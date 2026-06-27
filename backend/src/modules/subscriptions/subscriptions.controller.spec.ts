import {
  PERMISSIONS_KEY,
  PERMISSION_MODE_KEY,
} from '../../common/decorators/require-permissions.decorator';
import { PERMISSION_CODES } from '../../common/permissions/permission-codes';
import { SubscriptionsController } from './subscriptions.controller';

describe('SubscriptionsController permissions', () => {
  const methodPermissions = (methodName: keyof SubscriptionsController) =>
    Reflect.getMetadata(
      PERMISSIONS_KEY,
      SubscriptionsController.prototype[methodName],
    );

  const methodMode = (methodName: keyof SubscriptionsController) =>
    Reflect.getMetadata(
      PERMISSION_MODE_KEY,
      SubscriptionsController.prototype[methodName],
    );

  it('requires members permission for subscription management routes', () => {
    expect(methodPermissions('list')).toEqual([PERMISSION_CODES.MEMBERS]);
    expect(methodPermissions('getOne')).toEqual([PERMISSION_CODES.MEMBERS]);
    expect(methodPermissions('renew')).toEqual([PERMISSION_CODES.MEMBERS]);
    expect(methodPermissions('extend')).toEqual([PERMISSION_CODES.MEMBERS]);
    expect(methodPermissions('upgrade')).toEqual([PERMISSION_CODES.MEMBERS]);
    expect(methodPermissions('freeze')).toEqual([PERMISSION_CODES.MEMBERS]);
    expect(methodPermissions('unfreeze')).toEqual([PERMISSION_CODES.MEMBERS]);
    expect(methodPermissions('cancel')).toEqual([PERMISSION_CODES.MEMBERS]);
  });

  it('keeps invoice generation available to member or payment managers', () => {
    expect(methodPermissions('issueInvoice')).toEqual([
      PERMISSION_CODES.MEMBERS,
      PERMISSION_CODES.PAYMENTS,
    ]);
    expect(methodMode('issueInvoice')).toBe('any');
  });

  it('uses service-level permission resolution for compat creation', () => {
    expect(methodPermissions('createCompat')).toBeUndefined();
  });
});
