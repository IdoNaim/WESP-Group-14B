export function getUserFriendlyError(error: any): string {
    const message =
        error?.response?.data?.message ||
        error?.message ||
        '';

    return message
        .replace(/^[a-zA-Z0-9_.]+(?:Exception|Error):\s*/i, '')
        .trim();
}